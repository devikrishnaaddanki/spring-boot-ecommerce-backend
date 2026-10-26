package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message ="Username is Required")
    private String username;

    @NotBlank(message ="Password is required")
    @Size(min =6,message ="Password must contain atleast 6 characters")
    private String password;
}
