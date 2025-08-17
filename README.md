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

In addition to other development tasks, I expanded the functionality of the database layer by implementing new DAO (Data Access Object) methods. These methods provide additional capabilities for interacting with the database and managing data.

I also integrated these new DAO methods into the ViewModel layer, allowing the ViewModel to access and manipulate data from the database more effectively. By doing so, I improved the overall architecture of the application and enabled smoother data flow between different components.

While these enhancements may not yet be fully utilized in the application's user interface or other components, they lay the groundwork for future feature implementations and contribute to the overall robustness and flexibility of the application.

## Supabase Integration and Synchronization with Room

I also implemented Supabase integration to extend database capabilities beyond the local storage. Data is stored locally using Room and synchronized with Supabase whenever an internet connection is available, ensuring consistency between local and remote data sources.

## My DAO Testing Contributions

In addition to implementing new DAO (Data Access Object) methods, I conducted testing to ensure their reliability and correctness. Specifically, I tested the DAO methods to validate their functionality and interactions with the database.
