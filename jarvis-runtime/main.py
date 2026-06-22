# /// script
# dependencies = [
#   "requests",
# ]
# ///

import os
import sqlite3
import requests
import sys

def load_env(env_path):
    if os.path.exists(env_path):
        with open(env_path) as f:
            for line in f:
                line = line.strip()
                if '=' in line and not line.startswith('#'):
                    k, v = line.split('=', 1)
                    os.environ[k.strip()] = v.strip()

# Load env variables from root .env or local .env
load_env("../.env")
load_env(".env")

SUPABASE_URL = os.environ.get("SUPABASE_URL", "https://tbwnyuampjoamgarwwoo.supabase.co")
ANON_KEY = os.environ.get("SUPABASE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRid255dWFtcGpvYW1nYXJ3d29vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE5MzUwOTYsImV4cCI6MjA5NzUxMTA5Nn0.3CdCtROBH2l0wq8GVir9_3rWWZUtD9w2UWsz9caM3cg")
BUCKET_NAME = os.environ.get("SUPABASE_BUCKET", "jarvis-signals")
DB_PATH = "jarvis.db"

HEADERS = {
    "apikey": ANON_KEY,
    "Authorization": f"Bearer {ANON_KEY}",
    "Content-Type": "application/json"
}

def init_db(db_path):
    print(f"Initializing local SQLite database at: {os.path.abspath(db_path)}")
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS mobile_signals (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            device_id TEXT,
            source TEXT,
            sender TEXT,
            message TEXT,
            timestamp INTEGER,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(device_id, source, sender, message, timestamp)
        )
    """)
    conn.commit()
    conn.close()

def list_files(user_folder):
    url = f"{SUPABASE_URL}/storage/v1/object/list/{BUCKET_NAME}"
    payload = {
        "prefix": f"{user_folder}/",
        "limit": 100,
        "sortBy": {
            "column": "name",
            "order": "asc"
        }
    }
    
    try:
        response = requests.post(url, headers=HEADERS, json=payload)
        if response.status_code != 200:
            print(f"Failed to list files for {user_folder} (HTTP {response.status_code}): {response.text}")
            return []
            
        files_data = response.json()
        file_names = []
        for file_info in files_data:
            name = file_info.get("name", "")
            # Skip the archive directory entry, or items with no metadata
            if name and name != "archive" and not file_info.get("metadata") is None:
                file_names.append(name)
        return file_names
    except Exception as e:
        print(f"Error listing files in folder {user_folder}: {e}")
        return []

def download_file(user_folder, file_name):
    url = f"{SUPABASE_URL}/storage/v1/object/{BUCKET_NAME}/{user_folder}/{file_name}"
    try:
        response = requests.get(url, headers={
            "apikey": ANON_KEY,
            "Authorization": f"Bearer {ANON_KEY}"
        })
        if response.status_code != 200:
            print(f"Failed to download {user_folder}/{file_name} (HTTP {response.status_code})")
            return None
        return response.json()
    except Exception as e:
        print(f"Error downloading file {user_folder}/{file_name}: {e}")
        return None

def insert_signals(db_path, signals):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    inserted_count = 0
    
    for sig in signals:
        device_id = sig.get("deviceId", "unknown_device")
        source = sig.get("source", "unknown_source")
        sender = sig.get("sender", "unknown_sender")
        message = sig.get("message", "")
        timestamp = sig.get("timestamp", 0)
        
        try:
            cursor.execute("""
                INSERT OR IGNORE INTO mobile_signals (device_id, source, sender, message, timestamp)
                VALUES (?, ?, ?, ?, ?)
            """, (device_id, source, sender, message, timestamp))
            
            if cursor.rowcount > 0:
                inserted_count += 1
        except Exception as e:
            print(f"Failed to insert signal {sig}: {e}")
            
    conn.commit()
    conn.close()
    return inserted_count

def archive_file(user_folder, file_name):
    source_key = f"{user_folder}/{file_name}"
    dest_key = f"{user_folder}/archive/{file_name}"
    print(f"Archiving {source_key} to {dest_key}...")
    copy_url = f"{SUPABASE_URL}/storage/v1/object/copy"
    copy_payload = {
        "bucketId": BUCKET_NAME,
        "sourceKey": source_key,
        "destinationKey": dest_key
    }
    
    try:
        # 1. Copy file to archive subfolder
        copy_res = requests.post(copy_url, headers=HEADERS, json=copy_payload)
        if copy_res.status_code not in (200, 201):
            print(f"Failed to copy file to archive (HTTP {copy_res.status_code}): {copy_res.text}")
            return False
            
        # 2. Delete source file
        delete_url = f"{SUPABASE_URL}/storage/v1/object/{BUCKET_NAME}"
        delete_payload = {
            "prefixes": [source_key]
        }
        delete_res = requests.delete(delete_url, headers=HEADERS, json=delete_payload)
        if delete_res.status_code not in (200, 204):
            print(f"Failed to delete original file (HTTP {delete_res.status_code}): {delete_res.text}")
            return False
            
        print(f"Successfully archived {source_key}")
        return True
    except Exception as e:
        print(f"Error archiving file {source_key}: {e}")
        return False

def main():
    print("=== Jarvis Laptop Consumer Ingestion ===")
    init_db(DB_PATH)
    
    user_folders = ["pradeep", "shobana"]
    
    for user in user_folders:
        print(f"\n--- Checking folder for user: {user} ---")
        files = list_files(user)
        if not files:
            print(f"No new signal files found for user {user}.")
            continue
            
        print(f"Found {len(files)} files to process for user {user}.")
        for file_name in files:
            print(f"\nProcessing file: {user}/{file_name}")
            content = download_file(user, file_name)
            if not content:
                continue
                
            signals = content.get("signals", [])
            if not signals:
                print("File is empty or contains no signals list.")
                # Archive empty files to prevent reprocessing loops
                archive_file(user, file_name)
                continue
                
            print(f"Extracted {len(signals)} signals. Loading to SQLite...")
            new_inserted = insert_signals(DB_PATH, signals)
            print(f"Ingested {new_inserted} new unique signals (ignored {len(signals) - new_inserted} duplicates).")
            
            # Archiving file from landing zone after successful DB insertion
            archive_file(user, file_name)
            
    print("\nProcessing run complete.")

if __name__ == "__main__":
    main()
