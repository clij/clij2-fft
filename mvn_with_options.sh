# simple script that calls maven with the options we need, main purpose is to help us remember the options

# right now the main obtuse option we need to remember (when building locally) is to skip gpg signing
mvn install -DskipTests -Dgpg.skip -Dmaven.javadoc.skip=true -Denforcer.skip=true
