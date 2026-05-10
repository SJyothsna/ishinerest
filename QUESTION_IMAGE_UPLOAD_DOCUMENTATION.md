# Question Image Upload Feature Documentation

## Overview
This feature allows uploading images for questions. Images are stored in a structured directory format and URLs are saved in the database.

## Database Changes

### New Column Added
- **Table**: `questions`
- **Column**: `question_image_url` (VARCHAR(500), nullable)
- **Purpose**: Stores the URL to the question image

### Migration Script
Run this SQL in H2 Console (http://localhost:8080/h2-console):
```sql
ALTER TABLE questions ADD COLUMN question_image_url VARCHAR(500);
```

## File Storage Structure

Images are stored in the following directory structure:
```
public/uploads/chapters/{chapterId}/questions/
```

Example:
```
public/uploads/chapters/LC5H0102/questions/q_abc123.png
```

## API Endpoints

### 1. Upload Question Image
**Endpoint**: `POST /api/question-images/upload`

**Request**:
- Content-Type: `multipart/form-data`
- Parameters:
  - `file`: Image file (PNG, JPG, GIF, etc.)
  - `chapterId`: Chapter ID (e.g., "LC5H0102")

**Response** (Success - 200):
```json
{
  "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_abc123.png",
  "filename": "q_abc123.png",
  "message": "Image uploaded successfully"
}
```

**Response** (Error - 400):
```json
{
  "error": "File is empty"
}
```
or
```json
{
  "error": "File must be an image"
}
```

### 2. Delete Question Image
**Endpoint**: `DELETE /api/question-images/delete`

**Request**:
- Query Parameter: `imageUrl` (full URL of the image to delete)

**Example**:
```
DELETE /api/question-images/delete?imageUrl=http://localhost:8080/uploads/chapters/LC5H0102/questions/q_abc123.png
```

**Response** (Success - 200):
```json
{
  "message": "Image deleted successfully"
}
```

**Response** (Error - 404):
```json
{
  "error": "Image not found"
}
```

## Usage in Admin Panel

### Adding a Question with Image

1. **Select Subject and Chapter** from the dropdowns
2. **Fill in question details** (text, options, correct answer, etc.)
3. **Upload Image** (optional):
   - Click "Choose File" in the "Question Image" field
   - Select an image file (PNG, JPG, GIF)
   - Preview will appear automatically
   - Click "Remove Image" if you want to change or remove it
4. **Submit the form**
   - Image will be uploaded first
   - Question will be created with the image URL

### How It Works

1. When you select an image file, a preview is shown immediately
2. When you submit the form:
   - The image is uploaded to `/api/question-images/upload`
   - The server creates the directory structure if it doesn't exist
   - The image is saved with a unique filename (e.g., `q_uuid.png`)
   - The server returns the image URL
   - The question is created/updated with the image URL

## Entity Changes

### Question.java
Added new field:
```java
@Column
private String questionImageUrl; // URL to the question image
```

## Frontend Changes

### admin.html
1. **New Form Field**:
   - File input for image selection
   - Hidden input for storing the uploaded image URL
   - Image preview section
   - Remove image button

2. **New JavaScript Functions**:
   - `previewQuestionImage(event)`: Shows preview when image is selected
   - `removeQuestionImage()`: Clears the image selection and preview
   - Updated `clearQuestionFields()`: Clears image-related fields

3. **Updated Form Submission**:
   - Checks if an image file is selected
   - Uploads the image before creating/updating the question
   - Includes `questionImageUrl` in the question data

## Testing

### Using HTTP Client

See `src/test/java/rest/questionimages/questionimages.http` for test requests.

### Manual Testing Steps

1. **Run the database migration**:
   ```sql
   ALTER TABLE questions ADD COLUMN question_image_url VARCHAR(500);
   ```

2. **Restart the application** to load the new controller

3. **Open Admin Panel**: http://localhost:8080/admin

4. **Navigate to Questions tab**

5. **Click "Add Question"**

6. **Fill in the form and upload an image**

7. **Submit and verify**:
   - Check that the question is created successfully
   - Verify the image is stored in the correct directory
   - Check the database to see the `question_image_url` value

## File Naming Convention

Images are saved with the following naming pattern:
```
q_{UUID}.{extension}
```

Example: `q_a1b2c3d4-e5f6-7890-abcd-ef1234567890.png`

This ensures:
- Unique filenames (no conflicts)
- Easy identification (prefix "q_" for question)
- Original file extension is preserved

## Directory Creation

The system automatically creates the directory structure if it doesn't exist:
```
public/uploads/chapters/{chapterId}/questions/
```

## Security Considerations

1. **File Type Validation**: Only image files are accepted (checked via Content-Type)
2. **Unique Filenames**: UUID-based naming prevents file overwrites
3. **Directory Isolation**: Each chapter has its own directory

## Error Handling

The system handles the following error scenarios:
- Empty file upload
- Non-image file upload
- Directory creation failures
- File write failures
- Image not found during deletion

## Future Enhancements

Potential improvements:
1. Image size validation (max file size)
2. Image dimension validation
3. Image compression/optimization
4. Support for multiple images per question
5. Image editing capabilities (crop, resize)
6. CDN integration for better performance

## Made with Bob