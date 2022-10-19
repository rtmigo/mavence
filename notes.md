
```bash
VERSION=$(rtmaven local)

./runtest.sh $VERSION
```

To quickly configure the variables in local Bash session, you can edit the
following and then just paste to the terminal. Private data will not get into
the Bash history.

```bash
read -d '' MAVEN_GPG_KEY << EOF
-----BEGIN PGP PRIVATE KEY BLOCK-----
# you key here
-----END PGP PRIVATE KEY BLOCK-----
EOF
history -d $(history 2); history -d $(history 1)

MAVEN_GPG_PASSWORD="..."; history -d $(history 1)
SONATYPE_USERNAME="..."; history -d $(history 1)
SONATYPE_PASSWORD="..."; history -d $(history 1)

export SONATYPE_USERNAME
export SONATYPE_PASSWORD
export MAVEN_GPG_PASSWORD
export MAVEN_GPG_KEY
```