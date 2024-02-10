package com.ishine.ishinerest.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Question {
    private String id;
    private String question;
    private int type;
    private String answer;
    private String ch1;
    private String ch2;
    private String ch3;
    private String ch4;
    private int complexity;
    private long topic;
}
