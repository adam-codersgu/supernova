# supernova
A Kotlin-based Android application that allows the user to play music from their device while custom animations fall over the album artwork of the currently playing song.


## Test coverage
Target test coverage is 90%.

Run tests
`./gradlew test`

Generate a report
`./gradlew :app:jacocoUnitTestReport`

View the report
`./app/build/reports/jacoco/jacocoUnitTestReport/html/index.html`

### Troubleshooting

If you see this error:

```* What went wrong:
Execution failed for task ':app:bundleDebugClassesToCompileJar'.
> java.nio.file.FileSystemException: C:\Users\AndroidStudioProjects\supernova\app\build\intermediates\compile_app_classes_jar\debug\bundleDebugClassesToCompileJar\classes.jar: The process cannot access the file because it is being used by another process
```

Then run:

`taskkill /im java.exe /f`