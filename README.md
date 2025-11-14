Shared To-Do List (React + Simulated API Backend)

This is a real-time, collaborative To-Do list application built with React and Firebase Firestore, featuring a simulated layer for integrating with a hypothetical Java Spring Boot or Jakarta EE API.

The application demonstrates how a modern frontend can switch between direct database interaction and fetching data through a RESTful API layer, ensuring the UI remains responsive and functional in both scenarios.

✨ Features

Real-time Collaboration: Tasks update instantly for all active users (via Firestore persistence).

Authentication: Uses anonymous Firebase authentication for immediate use.

Data Source Toggle: A button allows switching the data source between:

Direct (Firestore): Fetches task data directly from the database.

Simulated API (Java): Fetches data via a mock fetchTasksFromApi function, which simulates API latency (500ms delay) and transforms the data format (e.g., changes completed: boolean to status: 'DONE' | 'PENDING'), mimicking a call to a Java backend.

Responsive Design: Styled using Tailwind CSS classes for a clean and mobile-friendly interface.

🛠️ Technology Stack

Frontend: React (JSX/Functional Components, Hooks)

Styling: Tailwind CSS

Database/Auth: Firebase Firestore and Authentication

Backend Simulation: JavaScript functions simulating a RESTful Java API endpoint.

🚀 Getting Started

Since this application is designed to run in a specific hosted environment (which provides the Firebase configuration and authentication tokens globally), setting it up locally requires minor modifications.
