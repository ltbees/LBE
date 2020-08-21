FROM unprotocol/un-gradle

RLBE set -o errexit -o nounset \
    && echo "git clone" \
    && git clone https://github.com/unprotocol/java-un.git \
    && cd java-un \
    && gradle build

WORKDIR /java-un

EXPOSE 18888