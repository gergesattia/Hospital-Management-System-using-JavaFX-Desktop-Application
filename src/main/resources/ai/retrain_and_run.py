import sys
from face_engine import engine

print("==========================================")
print("  MediCore AI Bridge - Retraining")
print("==========================================")
print()

success, message = engine.train()

if success:
    print()
    print(f"[OK] Retraining complete! Face database updated.")
    print(f"[INFO] {message}")
else:
    print()
    print(f"[ERROR] Retraining failed: {message}")

print()
print("[INFO] Starting AI Bridge now...")
print("==========================================")

# Import and start the Flask app directly (no path issues)
from ai_bridge import app
app.run(host='127.0.0.1', port=5005)
