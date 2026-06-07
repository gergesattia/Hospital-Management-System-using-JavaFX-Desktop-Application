import cv2
import os
import sys
import time

def main():
    if len(sys.argv) < 2:
        print("Usage: python enroll_standalone.py <username>")
        sys.exit(1)
        
    username = sys.argv[1].strip().lower()
    
    from face_engine import engine
    
    user_dir = os.path.join(engine.dataset_path, username)
    os.makedirs(user_dir, exist_ok=True)
    
    print(f"==========================================")
    print(f" MediCore Standalone Face Enrollment")
    print(f" User: {username}")
    print(f"==========================================")
    print("Press SPACE to capture a photo (5 required). Press ESC to cancel.")
    
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("Error: Could not open camera.")
        sys.exit(1)
        
    count = 0
    while count < 5:
        ret, frame = cap.read()
        if not ret:
            break
            
        display_frame = frame.copy()
        cv2.putText(display_frame, f"Captured: {count}/5 (Press SPACE)", (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        cv2.imshow("Enrollment", display_frame)
        
        key = cv2.waitKey(1)
        if key == 27: # ESC
            print("Enrollment cancelled.")
            cap.release()
            cv2.destroyAllWindows()
            sys.exit(0)
        elif key == 32: # SPACE
            img_path = os.path.join(user_dir, f"shot_{int(time.time())}.jpg")
            cv2.imwrite(img_path, frame)
            count += 1
            print(f"Captured {count}/5")
            
    cap.release()
    cv2.destroyAllWindows()
    
    print("\nTraining on new faces...")
    success, message = engine.train_user(username)
    if success:
        print(f"SUCCESS: {message}")
    else:
        print(f"FAILED: {message}")

if __name__ == "__main__":
    main()
