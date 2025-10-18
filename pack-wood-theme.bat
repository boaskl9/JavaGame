@echo off
echo Packing Wood Theme UI textures...
gradlew.bat :core:compileJava
java -cp "core\build\libs\*;core\build\classes\java\main" com.game.tools.TexturePackerTool
echo Done!
pause
