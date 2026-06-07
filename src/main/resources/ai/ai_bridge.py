import os
import base64
import cv2
import numpy as np
from flask import Flask, request, jsonify
from face_engine import engine

app = Flask(__name__)

@app.route('/', methods=['GET'])
def home():
    return jsonify({
        "status": "online",
        "message": "MediCore AI Bridge is running",
        "model": engine.model_name,
        "endpoints": ["/verify", "/enroll", "/health", "/retrain"]
    })

@app.route('/verify', methods=['POST'])
def verify():
    data = request.json
    username = data.get('username')
    image_b64 = data.get('image')
    
    if not username or not image_b64:
        return jsonify({"success": False, "message": "Missing username or image"}), 400
    
    try:
        # Decode image
        encoded_data = image_b64.split(',')[1] if ',' in image_b64 else image_b64
        nparr = np.frombuffer(base64.b64decode(encoded_data), np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if frame is None:
            return jsonify({"success": False, "message": "Invalid image data"}), 400
            
        # Verify
        is_match, distance, status = engine.verify_face(frame, username)
        print(f"[AI Bridge] Verify request for {username}: Match={is_match}, Dist={distance}, Status={status}")
        
        return jsonify({
            "success": True,
            "match": is_match,
            "distance": float(distance),
            "status": status
        })
    except Exception as e:
        print(f"[AI Bridge] Verify error: {e}")
        return jsonify({"success": False, "message": str(e)}), 500

@app.route('/enroll', methods=['POST'])
def enroll():
    data = request.json
    username = data.get('username')
    images_b64 = data.get('images', []) # Expect list of base64 images
    
    if not username or not images_b64:
        return jsonify({"success": False, "message": "Missing username or images"}), 400
        
    try:
        # Create dataset folder
        user_dir = os.path.join(engine.dataset_path, username)
        os.makedirs(user_dir, exist_ok=True)
        
        # Save images
        for i, img_b64 in enumerate(images_b64):
            encoded_data = img_b64.split(',')[1] if ',' in img_b64 else img_b64
            nparr = np.frombuffer(base64.b64decode(encoded_data), np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            if frame is not None:
                cv2.imwrite(os.path.join(user_dir, f"shot_{i}.jpg"), frame)
                
        # Train
        success, message = engine.train_user(username)
        return jsonify({"success": success, "message": message})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "model": engine.model_name})

@app.route('/retrain', methods=['POST'])
def retrain():
    try:
        success, message = engine.train()
        return jsonify({"success": success, "message": message})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500

if __name__ == '__main__':
    # Default port 5005 to avoid conflict with standard 5000
    print("[AI Bridge] Starting Flask AI Service on port 5005...")
    app.run(host='127.0.0.1', port=5005)
