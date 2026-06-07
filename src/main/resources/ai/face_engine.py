import cv2
import numpy as np
import os
import pickle
from deepface import DeepFace


class FaceEngine:
    def __init__(self, encodings_path=None, model_name="Facenet512", threshold=0.35):
        if encodings_path is None:
            self.encodings_path = os.path.join(
                os.path.dirname(os.path.abspath(__file__)), "encodings.pkl"
            )
        else:
            self.encodings_path = encodings_path
            
        self.dataset_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "dataset")
        self.model_name = model_name
        self.threshold = threshold
        self.known_encodings = []
        self.known_names = []
        self.load_encodings()

    def load_encodings(self):
        if os.path.exists(self.encodings_path):
            try:
                with open(self.encodings_path, "rb") as f:
                    data = pickle.load(f)
                    self.known_names = data["names"]
                    # Pre-normalize for faster cosine matching
                    self.known_encodings = []
                    for emb in data["encodings"]:
                        v = np.array(emb, dtype=np.float32)
                        norm = np.linalg.norm(v)
                        if norm > 0:
                            v = v / norm
                        self.known_encodings.append(v)
                print(
                    f"[FaceEngine] Loaded {len(self.known_names)} face encodings.",
                    flush=True,
                )
            except Exception as e:
                print(f"[FaceEngine] Error loading encodings: {e}", flush=True)
        else:
            print(
                "[FaceEngine] WARN: Encodings file not found! Please run training.",
                flush=True,
            )

    def verify_face(self, frame, expected_username):
        """
        Verifies if the face in the frame matches the expected_username.
        Includes a check for 'looking at camera'.
        """
        if not self.known_encodings:
            return False, 1.0, "No encodings loaded"

        try:
            # 1. Detect face and landmarks
            results = DeepFace.represent(
                img_path=frame,
                model_name=self.model_name,
                detector_backend="mtcnn",
                enforce_detection=False,
            )

            if not results or len(results) == 0:
                return False, 1.0, "No face detected"

            res = results[0]
            landmarks = res.get("facial_area", {})
            # RetinaFace landmarks check for 'looking at camera'
            # Simple heuristic: Nose should be relatively centered between eyes
            # landmarks: {x, y, w, h, left_eye, right_eye, nose, ...}
            # Note: DeepFace retinaface backend returns landmarks in 'facial_area' or 'landmarks'
            
            # Since DeepFace's structure varies, let's use the 'face_confidence' and a simple alignment check
            # For simplicity and reliability, we'll check if the face is too tilted
            # A more robust gaze detection would use specialized models, but we'll use 'nose-eye' alignment.
            
            # Let's assume looking away if detection score is low or alignment is off
            # In DeepFace + RetinaFace, 'landmarks' are often under res['landmarks']
            lks = res.get("landmarks")
            if lks:
                left_eye = lks['left_eye']
                right_eye = lks['right_eye']
                nose = lks['nose']
                
                # Calculate horizontal 'centeredness' of nose between eyes
                eye_dist = abs(right_eye[0] - left_eye[0])
                nose_rel_pos = (nose[0] - left_eye[0]) / (eye_dist + 1e-6)
                
                # If nose is not between 15% and 85% of the way between eyes, they are likely looking sideways
                if nose_rel_pos < 0.15 or nose_rel_pos > 0.85:
                    print(f"[FaceEngine] User looking away (Nose Pos: {nose_rel_pos:.2f})")
                    return False, 0.0, "Please look at the camera"

            # 2. Filter encodings for user
            expected_username = str(expected_username).strip().lower()
            user_indices = [i for i, name in enumerate(self.known_names) if name == expected_username]
            if not user_indices: return False, 1.0, "User not enrolled"
            user_db = np.stack([self.known_encodings[i] for i in user_indices])

            # 3. Get embedding and normalize
            target_emb = np.array(res["embedding"], dtype=np.float32)
            norm = np.linalg.norm(target_emb)
            if norm > 0: target_emb = target_emb / norm

            # 4. Compare
            similarities = user_db @ target_emb
            distances = 1 - similarities
            min_dist = float(np.min(distances))

            is_match = min_dist < self.threshold
            print(f"[DEBUG] User: {expected_username} | Dist: {min_dist:.4f} | Match: {is_match}")
            return is_match, min_dist, expected_username

        except Exception as e:
            print(f"[FaceEngine Error] {e}", flush=True)
            return False, 1.0, str(e)

    def train_user(self, username):
        """
        Incremental training: only trains one user and updates the encodings.
        """
        username = str(username).strip().lower()
        user_dir = os.path.join(self.dataset_path, username)
        
        if not os.path.exists(user_dir):
            return False, f"Folder for {username} not found."

        print(f"[FaceEngine] Incrementally training user: {username}", flush=True)
        
        # 1. Remove existing encodings for this user to avoid duplicates
        indices_to_keep = [i for i, name in enumerate(self.known_names) if name != username]
        new_names = [self.known_names[i] for i in indices_to_keep]
        # known_encodings is a list of arrays (or empty)
        new_encodings = [self.known_encodings[i] for i in indices_to_keep]

        # 2. Extract new embeddings
        added_count = 0
        for img_name in os.listdir(user_dir):
            img_path = os.path.join(user_dir, img_name)
            try:
                results = DeepFace.represent(
                    img_path=img_path,
                    model_name=self.model_name,
                    detector_backend="mtcnn",
                    enforce_detection=True
                )
                if results:
                    emb = np.array(results[0]["embedding"], dtype=np.float32)
                    norm = np.linalg.norm(emb)
                    if norm > 0: emb = emb / norm
                    
                    new_encodings.append(emb)
                    new_names.append(username)
                    added_count += 1
            except Exception as e:
                print(f"  [!] Skip image {img_name}: {e}")

        if added_count > 0:
            self.known_encodings = new_encodings
            self.known_names = new_names
            # Save to disk
            data = {"encodings": self.known_encodings, "names": self.known_names}
            with open(self.encodings_path, "wb") as f:
                pickle.dump(data, f)
            print(f"[FaceEngine] Incremental training complete. Added {added_count} signatures for {username}.", flush=True)
            return True, f"Successfully trained {username} ({added_count} images)."
        
        return False, f"No valid faces found for {username}."

    def delete_user(self, username):
        """
        Deletes a user's dataset folder and removes their encodings.
        """
        username = str(username).strip().lower()
        user_dir = os.path.join(self.dataset_path, username)
        
        if os.path.exists(user_dir):
            try:
                import shutil
                shutil.rmtree(user_dir)
                print(f"[FaceEngine] Deleted dataset for {username}")
            except Exception as e:
                print(f"[FaceEngine] Error deleting folder: {e}")

        indices_to_keep = [i for i, name in enumerate(self.known_names) if name != username]
        if len(indices_to_keep) < len(self.known_names):
            self.known_names = [self.known_names[i] for i in indices_to_keep]
            self.known_encodings = [self.known_encodings[i] for i in indices_to_keep]
            data = {"encodings": self.known_encodings, "names": self.known_names}
            with open(self.encodings_path, "wb") as f:
                pickle.dump(data, f)
            print(f"[FaceEngine] Removed {username} from encodings.pkl")
            return True
        return False

    def train(self):
        """
        Full retraining of the entire dataset.
        """
        print("[FaceEngine] Starting full retraining...", flush=True)
        all_encodings = []
        all_names = []

        if not os.path.exists(self.dataset_path):
            os.makedirs(self.dataset_path)
            return False, "Dataset folder created."

        for username in os.listdir(self.dataset_path):
            user_dir = os.path.join(self.dataset_path, username)
            if not os.path.isdir(user_dir): continue
            
            for img_name in os.listdir(user_dir):
                img_path = os.path.join(user_dir, img_name)
                try:
                    results = DeepFace.represent(
                        img_path=img_path,
                        model_name=self.model_name,
                        detector_backend="mtcnn",
                        enforce_detection=True
                    )
                    if results:
                        emb = np.array(results[0]["embedding"], dtype=np.float32)
                        norm = np.linalg.norm(emb)
                        if norm > 0: emb = emb / norm
                        all_encodings.append(emb)
                        all_names.append(username)
                except: continue

        if all_encodings:
            self.known_encodings = all_encodings
            self.known_names = all_names
            data = {"encodings": self.known_encodings, "names": self.known_names}
            with open(self.encodings_path, "wb") as f:
                pickle.dump(data, f)
            return True, f"Full training complete. Total users: {len(set(all_names))}"
        return False, "No data found."


# Singleton instance for easy import
engine = FaceEngine()
