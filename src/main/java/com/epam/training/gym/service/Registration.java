package com.epam.training.gym.service;


public record Registration<T>(T profile, String rawPassword) {
}