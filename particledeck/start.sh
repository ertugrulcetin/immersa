#!/bin/bash
# Simple HTTP server for ParticleDeck
echo "Starting ParticleDeck on http://localhost:8080"
echo "Press Ctrl+C to stop"
python3 -m http.server 8080
