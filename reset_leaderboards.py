import firebase_admin
from firebase_admin import credentials, firestore

cred = credentials.Certificate("firebase-key.json")
firebase_admin.initialize_app(cred)

db = firestore.client()

TOKEN_REWARDS = {
    "leaderboard_weekly": {1: 5, 2: 3, 3: 2},
    "leaderboard_monthly": {1: 10, 2: 6, 3: 4}
}

def reward_tokens(collection, entries):
    rewards = TOKEN_REWARDS.get(collection, {})
    for i, doc in enumerate(entries):
        rank = i + 1
        if rank in rewards:
            uid = doc.id
            tokens = rewards[rank]
            user_ref = db.collection("users").document(uid)
            user_doc = user_ref.get()
            if user_doc.exists:
                current = user_doc.get("tokens") or 0
                user_ref.update({"tokens": current + tokens})
                print(f"  {collection} #{rank}: {doc.get('username')} +{tokens} tokens")
        elif rank <= 10:
            # 4-10 place: 1 weekly, 2 monthly
            tokens = 1 if "weekly" in collection else 2
            uid = doc.id
            user_ref = db.collection("users").document(uid)
            user_doc = user_ref.get()
            if user_doc.exists:
                current = user_doc.get("tokens") or 0
                user_ref.update({"tokens": current + tokens})
                print(f"  {collection} #{rank}: {doc.get('username')} +{tokens} tokens")

def reset_collection(name):
    snapshot = list(db.collection(name).order_by("stars", firestore.Query.DESCENDING).limit(10).stream())

    # Give rewards before deleting
    reward_tokens(name, snapshot)

    # Delete entries
    for doc in snapshot:
        doc.reference.delete()

    print(f"Cleared {name}")

reset_collection("leaderboard_weekly")
reset_collection("leaderboard_monthly")