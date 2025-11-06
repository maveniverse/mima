# MIMA Maven (configured) HttpClient 4.x

MIMA factory of Maven configured Apache HttpClient 4.x. It creates an ASF HttpClient 4.x instance configured "as Maven does", so all of Maven configuration/auth/proxy is applied to it.

As Maven Core does not exports any of Apache HttpClient 4.x, it is user duty to make sure all needed is present on classpath.
