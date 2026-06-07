import cv2
import numpy as np
import os
from face_engine import engine
from deepface import DeepFace

def start_live_test():
    # 1. Verify encodings exist
    if not engine.known_encodings:
        print("[ERROR] No face encodings found. Please run 'train_faces.py' first!")
        return

    # 2. Open Webcam
    cap = cv2.VideoCapture(0)
    
    if not cap.isOpened():
        print("[ERROR] Could not open webcam.")
        return

    print("\n" + "="*40)
    print("   BIOMETRIC LIVE RECOGNITION TEST")
    print("="*40)
    print("[INFO] Press 'Q' to exit.")
    print("[INFO] Threshold set to:", engine.threshold)

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # Flip for mirroring effect
        frame = cv2.flip(frame, 1)
        display_frame = frame.copy()
        
        try:
            # 3. Detect and Represent
            results = DeepFace.represent(
                img_path=frame,
                model_name=engine.model_name,
                detector_backend="opencv",
                enforce_detection=False
            )

            if results and len(results) > 0:
                for face in results:
                    target_emb = np.array(face["embedding"], dtype=np.float32)
                    norm = np.linalg.norm(target_emb)
                    if norm > 0: target_emb = target_emb / norm

                    # 4. Compare with Database
                    db_matrix = np.stack(engine.known_encodings)
                    similarities = db_matrix @ target_emb
                    distances = 1 - similarities
                    
                    best_idx = int(np.argmin(distances))
                    min_dist = float(distances[best_idx])
                    detected_id = str(engine.known_names[best_idx])
                    
                    # 5. Determine if it's a match
                    is_match = min_dist < engine.threshold
                    color = (0, 255, 0) if is_match else (0, 0, 255) # Green if match, Red if unknown
                    
                    label = f"ID: {detected_id}" if is_match else "Unknown"
                    match_percent = max(0, (1 - min_dist) * 100)
                    
                    # 6. Draw on frame
                    area = face["facial_area"]
                    cv2.rectangle(display_frame, (area['x'], area['y']), 
                                  (area['x']+area['w'], area['y']+area['h']), color, 2)
                    
                    # Add background for text for better readability
                    cv2.rectangle(display_frame, (area['x'], area['y'] - 30), 
                                  (area['x']+area['w'], area['y']), color, -1)
                    
                    cv2.putText(display_frame, f"{label} ({match_percent:.1f}%)", 
                                (area['x'] + 5, area['y'] - 10), 
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 2)
        
        except Exception as e:
            # Silence deepface errors during live stream
            pass

        # Show window
        cv2.imshow('Live Biometric Recognition', display_frame)

        # Break loop on 'q'
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    # Cleanup
    cap.release()
    cv2.destroyAllWindows()
    print("[INFO] Live test closed.")

if __name__ == "__main__":
    start_live_test()
