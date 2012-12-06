package ca.bc.gov.open.cpf.api.security.vote;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;

public abstract class AbstractPathPrefixVoter implements AccessDecisionVoter {
  private List<String> prefixes;

  public abstract int doVote(Authentication authentication, Object object,
    Collection<ConfigAttribute> config);

  public List<String> getPrefixes() {
    return prefixes;
  }

  public void setPrefixes(final List<String> prefixes) {
    this.prefixes = prefixes;
  }

  public void setPrefixes(final String... prefixes) {
    final List<String> prefixList = Arrays.asList(prefixes);
    setPrefixes(prefixList);
  }

  @Override
  public boolean supports(final Class<?> clazz) {
    return FilterInvocation.class.isAssignableFrom(clazz);
  }

  @Override
  public boolean supports(final ConfigAttribute attribute) {
    return true;
  }

  @Override
  public int vote(final Authentication authentication, final Object object,
    final Collection<ConfigAttribute> config) {
    if (object instanceof FilterInvocation) {
      final FilterInvocation filterInvocation = (FilterInvocation)object;
      final String path = filterInvocation.getRequestUrl();
      for (final String prefix : prefixes) {
        if (path.startsWith(prefix) && !path.equals(prefix)) {
          return doVote(authentication, object, config);
        }
      }
    }

    return ACCESS_ABSTAIN;
  }

}
