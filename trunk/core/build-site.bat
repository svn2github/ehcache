REM A recent Maven regression makes unhide very slow when run from Maven. Running it from ant as a workaround until I find what regressed.
mvn -Dmaven.test.skip=true clean package site

REM Now, Ant to unhide
ant unhide_html


