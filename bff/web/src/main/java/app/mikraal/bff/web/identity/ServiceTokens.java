package app.mikraal.bff.web.identity;

import app.mikraal.bff.web.directory.Person;

/** Bearer tokens that carry the caller's identity to the services. */
public interface ServiceTokens {

    String issueFor(Person person);
}
