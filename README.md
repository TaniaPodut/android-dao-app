# Bus Schedule App

This folder contains the source code for the Bus Schedule app codelab.

# Introduction
The Bus Schedule app displays a list of bus stops and arrival times. Tapping a bus stop on the first
screen will display a list of all arrival times for that particular stop.

The bus stops are stored in a Room database. Schedule items are represented by the `Schedule` class 
and queries on the data table are made by the `ScheduleDao` class. The app includes a view model to
access the `ScheduleDao` and format data to be display in a list.

# Pre-requisites
* Experience with Kotlin syntax.
* Experience with Jetpack Compose.
* How to create and run a project in Android Studio.
* Basic knowledge of SQL databases and performing basic queries.

# Getting Started
1. Install Android Studio, if you don't already have it.
2. Download the sample.
3. Import the sample into Android Studio.
4. Build and run the sample.

## My Accessibility Contributions

I prioritized accessibility features to ensure that the app is usable by a wide range of users, including those with disabilities. Here are the specific accessibility enhancements I implemented:

- **Bold Text**: Incorporated bold text where appropriate to improve readability for users with visual impairments.

- **Color Contrast**: Ensured that there is sufficient color contrast between text and background throughout the app. By enhancing color contrast, I aimed to improve readability for users with low vision or color vision deficiencies. This helps prevent text from blending into the background and enhances overall legibility.

- **Bigger Text**: Increased the text size in certain areas of the app to benefit users with visual impairments or those who prefer larger text for readability.

## My Room Database Contributions

-Added new DAO methods for extended data access.
-Integrated DAOs into the ViewModel to improve data flow.
-Enhanced database layer to support future features.

## My DAO Testing Contributions

- Tested new DAO methods for correctness and reliability.
- Validated insert, delete, and query operations.

## Supabase Integration and Synchronization with Room

- Integrated Supabase for remote data synchronization.
- Room handles local storage; data is synced with Supabase when online.
- Ensures consistency between local and remote data sources.

