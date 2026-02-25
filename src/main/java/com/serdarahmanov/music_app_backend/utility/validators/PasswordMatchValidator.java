package com.serdarahmanov.music_app_backend.utility.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch,Object> {

    private String passwordFieldName;
    private String passwordConfirmationFieldName;

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        this.passwordFieldName = constraintAnnotation.passwordField();
        this.passwordConfirmationFieldName = constraintAnnotation.passwordConfirmationField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Class<?> clazz = value.getClass();

            Field passwordField = clazz.getDeclaredField(passwordFieldName);
            Field passwordConfirmationField = clazz.getDeclaredField(passwordConfirmationFieldName);

            passwordField.setAccessible(true);
            passwordConfirmationField.setAccessible(true);

            String password = (String) passwordField.get(value);
            String passwordConfirmation = (String) passwordConfirmationField.get(value);

            boolean isValid = password != null && password.equals(passwordConfirmation);

            if (!isValid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Passwords do not match")
                        .addPropertyNode(passwordConfirmationFieldName)  // <-- KEY LINE
                        .addConstraintViolation();
            }

            return isValid;

        } catch (Exception e) {
            // Don't print stack trace in production
            return false;
        }
    }
}
