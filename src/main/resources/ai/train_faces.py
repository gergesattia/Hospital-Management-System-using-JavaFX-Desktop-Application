import os
import pickle
import numpy as np
from deepface import DeepFace
from tqdm import tqdm

# Configuration
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATASET_PATH = os.path.join(BASE_DIR, "dataset")
ENCODINGS_PATH = os.path.join(BASE_DIR, "encodings.pkl")
MODEL_NAME = "Facenet512"


def train():
    known_encodings = []
    known_names = []

    if not os.path.exists(DATASET_PATH):
        print(f"[ERROR] Dataset path '{DATASET_PATH}' does not exist!")
        return

    print(f"[INFO] Starting training with model: {MODEL_NAME}")

    folders = [
        f
        for f in os.listdir(DATASET_PATH)
        if os.path.isdir(os.path.join(DATASET_PATH, f))
    ]

    if not folders:
        print(
            "[WARN] No subdirectories found in dataset. Each subdirectory should be named with an SSN."
        )
        return

    for ssn in folders:
        folder_path = os.path.join(DATASET_PATH, ssn)
        print(f"\n[PROCESS] Folder: {ssn}")

        images = [
            f
            for f in os.listdir(folder_path)
            if f.lower().endswith((".png", ".jpg", ".jpeg"))
        ]
        if not images:
            print(f"  [SKIP] No images found in {ssn}")
            continue

        for img_name in images:
            img_path = os.path.join(folder_path, img_name)

            try:
                # Extract embedding
                results = DeepFace.represent(
                    img_path=img_path,
                    model_name=MODEL_NAME,
                    enforce_detection=True,
                    detector_backend="retinaface",
                )

                for res in results:
                    known_encodings.append(res["embedding"])
                    known_names.append(ssn)

                print(f"  [OK] {img_name}")

            except Exception as e:
                print(f"  [ERR] {img_name}: {str(e)[:50]}...")

    # Save to file
    if known_names:
        data = {"encodings": known_encodings, "names": known_names}
        with open(ENCODINGS_PATH, "wb") as f:
            pickle.dump(data, f)
        print(
            f"\n[SUCCESS] Training complete. Saved {len(known_names)} faces to {ENCODINGS_PATH}"
        )
    else:
        print("\n[ERROR] No faces were encoded. Check your dataset.")


if __name__ == "__main__":
    train()
