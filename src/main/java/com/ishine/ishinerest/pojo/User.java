package com.ishine.ishinerest.pojo;

import lombok.*;

import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {
    private String id;
    private String email;
    private String name;
    private String grade;
    private ArrayList<String> questions;
}
