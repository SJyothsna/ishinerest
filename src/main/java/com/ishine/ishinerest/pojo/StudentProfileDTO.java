// src/main/java/com/ishine/ishinerest/pojo/StudentProfileDTO.java
package com.ishine.ishinerest.pojo;

public record StudentProfileDTO(
                Long id,
                String name,
                String email,
                boolean hasClass,
                Integer classId,
                long subjectCount) {
}
