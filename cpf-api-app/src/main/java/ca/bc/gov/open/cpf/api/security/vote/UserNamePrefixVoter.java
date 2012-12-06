package ca.bc.gov.open.cpf.api.security.vote;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;

public class UserNamePrefixVoter implements AccessDecisionVoter {
  private List<String> prefixes;

  public List<String> getPrefixes() {
    return prefixes;
  }

  public void setPrefix(final String prefix) {
    setPrefixes(prefix);
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
          final String userName = authentication.getName();
          if (path.startsWith(prefix + userName)) {
            return ACCESS_GRANTED;
          } else {
            return ACCESS_DENIED;
          }
        }
      }

      return ACCESS_ABSTAIN;
    } else {
      return ACCESS_ABSTAIN;
    }
  }

}
