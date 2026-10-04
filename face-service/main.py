import cv2
import numpy as np
import json
from fastapi import FastAPI, File, UploadFile, Form
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from insightface.app import FaceAnalysis

# Global face analysis model instance
face_app = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan context manager for FastAPI.
    Loads the InsightFace model once on startup and keeps it in memory.
    """
    global face_app
    print("Loading InsightFace buffalo_l model. This may take a moment...")
    # Load 'buffalo_l' model using CPU execution provider (ctx_id=-1)
    face_app = FaceAnalysis(name='buffalo_l', providers=['CPUExecutionProvider'])
    face_app.prepare(ctx_id=-1, det_size=(640, 640))
    print("InsightFace model loaded successfully.")
    yield
    print("Shutting down Face Recognition Service.")

app = FastAPI(title="Face Recognition Service", lifespan=lifespan)

# Allow all origins for CORS (so the Java backend can call these endpoints)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def cosine_similarity(emb1, emb2):
    """Compute cosine similarity between two vectors."""
    return float(np.dot(emb1, emb2) / (np.linalg.norm(emb1) * np.linalg.norm(emb2)))

@app.get("/health")
async def health():
    """Health check endpoint to verify the service and model status."""
    return {"status": "ok", "model_loaded": face_app is not None}

@app.post("/register-face")
async def register_face(
    roll_no: str = Form(...),
    image: UploadFile = File(...)
):
    """
    Extracts the face embedding for a new student registration.
    Expects a single clear face in the image.
    """
    try:
        # Read the uploaded image file into an OpenCV image
        contents = await image.read()
        nparr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is None:
            return {"success": False, "error": "Invalid image file format."}

        # Run face detection and recognition
        faces = face_app.get(img)

        if len(faces) == 0:
            return {"success": False, "error": "No face detected in the image."}

        # Sort faces by bounding box area (largest first)
        faces = sorted(faces, key=lambda f: (f.bbox[2] - f.bbox[0]) * (f.bbox[3] - f.bbox[1]), reverse=True)
        largest_face = faces[0]

        # Check for ambiguous faces: reject if there are multiple faces and the 
        # second largest is at least half the size of the largest face.
        if len(faces) > 1:
            largest_area = (largest_face.bbox[2] - largest_face.bbox[0]) * (largest_face.bbox[3] - largest_face.bbox[1])
            second_largest = faces[1]
            second_area = (second_largest.bbox[2] - second_largest.bbox[0]) * (second_largest.bbox[3] - second_largest.bbox[1])
            
            if largest_area < second_area * 2.0:
                return {
                    "success": False, 
                    "error": "Multiple ambiguous faces found. Please upload a clear image of a single person."
                }

        # The 'embedding' field contains the 512-dim feature vector
        embedding = largest_face.embedding.tolist()

        return {
            "success": True,
            "embedding": embedding
        }

    except Exception as e:
        print(f"Error registering face: {e}")
        return {"success": False, "error": "Internal server error during face registration."}


@app.post("/recognize-group")
async def recognize_group(
    known_faces: str = Form(...),
    image: UploadFile = File(...)
):
    """
    Recognizes all faces in a group photo against a list of known faces.
    """
    try:
        # Parse the JSON string containing known students' embeddings
        try:
            known_data = json.loads(known_faces)
        except json.JSONDecodeError:
            return {"success": False, "error": "Invalid JSON format for known_faces."}

        # Read the group photo
        contents = await image.read()
        nparr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is None:
            return {"success": False, "error": "Invalid image file format."}

        image_height, image_width = img.shape[:2]

        # Detect all faces in the photo
        faces = face_app.get(img)
        results = []
        
        similarity_threshold = 0.45

        # Compare each detected face against the known faces
        for face in faces:
            bbox = face.bbox
            x = float(bbox[0])
            y = float(bbox[1])
            width = float(bbox[2] - bbox[0])
            height = float(bbox[3] - bbox[1])
            
            face_emb = face.embedding
            
            best_match = "unknown"
            best_conf = 0.0
            second_conf = 0.0
            second_best_roll = None
            
            # Find the closest matching known face
            for known in known_data:
                k_emb = np.array(known["embedding"])
                sim = cosine_similarity(face_emb, k_emb)
                
                if sim > best_conf:
                    second_conf = best_conf
                    second_best_roll = best_match if best_match != "unknown" else None
                    
                    best_conf = sim
                    best_match = known["roll_no"]
                elif sim > second_conf:
                    second_conf = sim
                    second_best_roll = known["roll_no"]
            
            if best_conf <= similarity_threshold:
                best_match = "unknown"
                
            is_matched = best_conf > similarity_threshold
            matched_student_id = best_match if is_matched else None
            
            ambiguous = False
            if is_matched and (best_conf - second_conf) < 0.10:
                ambiguous = True

            results.append({
                "bounding_box": {"x": x, "y": y, "width": width, "height": height},
                "matched_roll_no": best_match,
                "confidence": round(float(best_conf), 2),
                "bbox": [int(bbox[0]), int(bbox[1]), int(bbox[2]), int(bbox[3])],
                "student_id": matched_student_id,
                "matched": is_matched,
                "image_width": int(image_width),
                "image_height": int(image_height),
                "second_confidence": round(float(second_conf), 2),
                "second_best_roll_no": second_best_roll,
                "ambiguous": ambiguous
            })
            
        return results

    except Exception as e:
        print(f"Error recognizing group: {e}")
        return {"success": False, "error": "Internal server error during group recognition."}
