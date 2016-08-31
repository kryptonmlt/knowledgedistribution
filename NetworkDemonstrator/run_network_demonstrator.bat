set "THETA=0 0.001 0.01 0.1 0.3 0.5 0.7 1.0"
for %%i in (%THETA%) do (
java -jar target\NetworkDemonstrator-1.0-SNAPSHOT-jar-with-dependencies.jar %%i
)