FROM unprotocol/un-gradle

RLBE set -o errexit -o nounset \
#  Download and build java-un
    && echo "git clone" \
    && git clone https://github.com/unprotocol/java-un.git \
    && cd java-un \
    && gradle build

# Change work directory
WORKDIR /java-un

# open port 18888
EXPOSE 18888

