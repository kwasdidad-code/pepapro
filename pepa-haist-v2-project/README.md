# Pepa Haist 2.0

A lively, light Android WebView fitness app with:
- 365-day / 52-week mixed training progression
- daily posture and neck/upper-back mobility micro-block
- YouTube exercise embeds loaded from an HTTPS app origin to address modern YouTube WebView embedding requirements
- native Android camera bridge for Food and Progress photos
- gallery picker
- local progress, streak and meal logging
- free built-in Pepa Coach (rule-based; no paid AI key)
- animated Spotify-style now-playing visualizer and Spotify launcher

## Build
The repository includes `.github/workflows/build-apk.yml`.
GitHub Actions builds `app-debug.apk`.

## Spotify
The app can open Spotify. Reliable in-app track metadata/control requires Spotify's Android App Remote integration and a Spotify Developer Client ID. This project deliberately does not ship a secret/client ID.

## AI
The built-in coach is free and local. A real cloud LLM requires a secure backend or user-supplied API key; a secret should not be embedded in the APK.

## Safety
The posture content is general exercise guidance, not a diagnosis or guaranteed medical correction. Stop for pain, dizziness, numbness, or other concerning symptoms.
