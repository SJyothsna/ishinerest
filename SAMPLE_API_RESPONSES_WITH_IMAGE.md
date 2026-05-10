# Sample API Responses with Question Image URL

## 1. GET Single Question - Response

**Endpoint**: `GET /api/questions/{id}`

**Response** (Question WITHOUT image):
```json
{
  "questionId": 1,
  "questionText": "What is 2 + 2?",
  "optionA": "3",
  "optionB": "4",
  "optionC": "5",
  "optionD": "6",
  "optionE": null,
  "optionF": null,
  "correctAnswer": "B",
  "correctAnswers": null,
  "questionType": 1,
  "difficultyLevel": "Basic",
  "explanation": "2 + 2 equals 4",
  "notes": "Basic arithmetic",
  "createdBy": null,
  "tags": null,
  "sectionId": null,
  "usageType": "Both",
  "questionImageUrl": null,
  "chapter": {
    "chapterId": "LC5H0102",
    "chapterName": "Numbers",
    "sectionId": null,
    "sectionName": null
  }
}
```

**Response** (Question WITH image):
```json
{
  "questionId": 2,
  "questionText": "Solve the equation shown in the image:",
  "optionA": "x = 5",
  "optionB": "x = 10",
  "optionC": "x = 15",
  "optionD": "x = 20",
  "optionE": null,
  "optionF": null,
  "correctAnswer": "B",
  "correctAnswers": null,
  "questionType": 1,
  "difficultyLevel": "Intermediate",
  "explanation": "Solving the equation step by step gives x = 10",
  "notes": "Refer to the image for the complete equation",
  "createdBy": null,
  "tags": null,
  "sectionId": null,
  "usageType": "Both",
  "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "chapter": {
    "chapterId": "LC5H0102",
    "chapterName": "Numbers",
    "sectionId": null,
    "sectionName": null
  }
}
```

---

## 2. GET Questions by Chapter - Response

**Endpoint**: `GET /api/questions/chapter/{chapterId}`

**Response** (Array of questions, some with images):
```json
[
  {
    "questionId": 1,
    "questionText": "What is 2 + 2?",
    "optionA": "3",
    "optionB": "4",
    "optionC": "5",
    "optionD": "6",
    "correctAnswer": "B",
    "questionType": 1,
    "difficultyLevel": "Basic",
    "explanation": "2 + 2 equals 4",
    "notes": "Basic arithmetic",
    "usageType": "Both",
    "questionImageUrl": null,
    "chapter": {
      "chapterId": "LC5H0102",
      "chapterName": "Numbers"
    }
  },
  {
    "questionId": 2,
    "questionText": "Identify the shape in the image:",
    "optionA": "Circle",
    "optionB": "Square",
    "optionC": "Triangle",
    "optionD": "Rectangle",
    "correctAnswer": "C",
    "questionType": 1,
    "difficultyLevel": "Basic",
    "explanation": "The image shows a triangle",
    "notes": null,
    "usageType": "Practice",
    "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_shape123.png",
    "chapter": {
      "chapterId": "LC5H0102",
      "chapterName": "Numbers"
    }
  },
  {
    "questionId": 3,
    "questionText": "Calculate the area of the figure shown:",
    "optionA": "25 cm²",
    "optionB": "30 cm²",
    "optionC": "35 cm²",
    "optionD": "40 cm²",
    "correctAnswer": "B",
    "questionType": 1,
    "difficultyLevel": "Intermediate",
    "explanation": "Using the formula A = l × w",
    "notes": "Dimensions are shown in the image",
    "usageType": "Both",
    "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_area456.png",
    "chapter": {
      "chapterId": "LC5H0102",
      "chapterName": "Numbers"
    }
  }
]
```

---

## 3. POST Create Question - Request & Response

**Endpoint**: `POST /api/questions`

**Request** (Creating question WITH image):
```json
[
  {
    "questionText": "What geometric shape is shown in the diagram?",
    "optionA": "Pentagon",
    "optionB": "Hexagon",
    "optionC": "Octagon",
    "optionD": "Decagon",
    "correctAnswer": "B",
    "questionType": 1,
    "difficultyLevel": "Basic",
    "explanation": "The shape has 6 sides, making it a hexagon",
    "notes": "Count the sides in the image",
    "usageType": "Both",
    "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_abc123.png",
    "chapter": {
      "chapterId": "LC5H0102"
    }
  }
]
```

**Response** (Success):
```json
[
  {
    "questionId": 10,
    "questionText": "What geometric shape is shown in the diagram?",
    "optionA": "Pentagon",
    "optionB": "Hexagon",
    "optionC": "Octagon",
    "optionD": "Decagon",
    "optionE": null,
    "optionF": null,
    "correctAnswer": "B",
    "correctAnswers": null,
    "questionType": 1,
    "difficultyLevel": "Basic",
    "explanation": "The shape has 6 sides, making it a hexagon",
    "notes": "Count the sides in the image",
    "createdBy": null,
    "tags": null,
    "sectionId": null,
    "usageType": "Both",
    "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_abc123.png",
    "chapter": {
      "chapterId": "LC5H0102",
      "chapterName": "Numbers",
      "sectionId": null,
      "sectionName": null
    }
  }
]
```

---

## 4. PUT Update Question - Request & Response

**Endpoint**: `PUT /api/questions/{id}`

**Request** (Adding image to existing question):
```json
{
  "questionId": 5,
  "questionText": "Solve the equation shown below:",
  "optionA": "x = 3",
  "optionB": "x = 5",
  "optionC": "x = 7",
  "optionD": "x = 9",
  "correctAnswer": "C",
  "questionType": 1,
  "difficultyLevel": "Intermediate",
  "explanation": "Step-by-step solution leads to x = 7",
  "notes": "Refer to the diagram",
  "usageType": "Test",
  "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_equation789.png",
  "chapter": {
    "chapterId": "LC5H0102"
  }
}
```

**Response** (Success):
```json
{
  "questionId": 5,
  "questionText": "Solve the equation shown below:",
  "optionA": "x = 3",
  "optionB": "x = 5",
  "optionC": "x = 7",
  "optionD": "x = 9",
  "optionE": null,
  "optionF": null,
  "correctAnswer": "C",
  "correctAnswers": null,
  "questionType": 1,
  "difficultyLevel": "Intermediate",
  "explanation": "Step-by-step solution leads to x = 7",
  "notes": "Refer to the diagram",
  "createdBy": null,
  "tags": null,
  "sectionId": null,
  "usageType": "Test",
  "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_equation789.png",
  "chapter": {
    "chapterId": "LC5H0102",
    "chapterName": "Numbers",
    "sectionId": null,
    "sectionName": null
  }
}
```

---

## 5. Image Upload Response

**Endpoint**: `POST /api/question-images/upload`

**Request**:
- Form Data with `file` and `chapterId`

**Response** (Success):
```json
{
  "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "filename": "q_a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
  "message": "Image uploaded successfully"
}
```

---

## UI Implementation Notes

### For Displaying Questions with Images:

```javascript
// Check if question has an image
if (question.questionImageUrl) {
    // Display the image
    const img = document.createElement('img');
    img.src = question.questionImageUrl;
    img.alt = 'Question Image';
    img.style.maxWidth = '100%';
    img.style.height = 'auto';
    // Add to your question display container
}
```

### For Question Lists:

```javascript
questions.forEach(question => {
    const questionDiv = document.createElement('div');
    questionDiv.className = 'question-item';
    
    // Question text
    const questionText = document.createElement('p');
    questionText.textContent = question.questionText;
    questionDiv.appendChild(questionText);
    
    // Image (if exists)
    if (question.questionImageUrl) {
        const img = document.createElement('img');
        img.src = question.questionImageUrl;
        img.alt = 'Question Image';
        img.style.maxWidth = '300px';
        img.style.marginTop = '10px';
        questionDiv.appendChild(img);
    }
    
    // Add to container
    container.appendChild(questionDiv);
});
```

### For Editing Questions:

```javascript
// When loading question for edit
if (question.questionImageUrl) {
    // Show existing image
    document.getElementById('existingImagePreview').src = question.questionImageUrl;
    document.getElementById('existingImagePreview').style.display = 'block';
    
    // Store URL in hidden field
    document.getElementById('questionImageUrl').value = question.questionImageUrl;
}
```

---

## Key Points for UI Development

1. **Check for null**: Always check if `questionImageUrl` is not null before displaying
2. **Image sizing**: Use CSS to control image dimensions (max-width, max-height)
3. **Loading states**: Show loading indicator while image uploads
4. **Error handling**: Display error messages if image upload fails
5. **Preview**: Show image preview before submitting the form
6. **Responsive**: Ensure images are responsive on mobile devices

---

## Example HTML for Displaying Question with Image

```html
<div class="question-card">
    <h3>Question #1</h3>
    <p class="question-text">What geometric shape is shown in the diagram?</p>
    
    <!-- Image (only if exists) -->
    <div class="question-image" style="margin: 15px 0;">
        <img src="http://localhost:8080/uploads/chapters/LC5H0102/questions/q_abc123.png" 
             alt="Question Image" 
             style="max-width: 100%; height: auto; border: 1px solid #ddd; border-radius: 4px;">
    </div>
    
    <!-- Options -->
    <div class="options">
        <label><input type="radio" name="q1" value="A"> Pentagon</label>
        <label><input type="radio" name="q1" value="B"> Hexagon</label>
        <label><input type="radio" name="q1" value="C"> Octagon</label>
        <label><input type="radio" name="q1" value="D"> Decagon</label>
    </div>
</div>
```

---

## Made with Bob