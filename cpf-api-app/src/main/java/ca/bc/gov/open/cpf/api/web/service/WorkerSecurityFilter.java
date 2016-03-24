/*
 * Copyright © 2008-2016, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.api.web.service;

import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import ca.bc.gov.open.cpf.plugin.impl.security.SignatureUtil;

import com.revolsys.ui.web.utils.HttpServletUtils;

public class WorkerSecurityFilter extends OncePerRequestFilter {
  @Resource(name = "userAccountSecurityService")
  private UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
    final HttpServletResponse response, final FilterChain filterChain)
    throws ServletException, IOException {
    try {
      final String workerUserName = request.getParameter("workerUsername");
      final UserDetails userDetails = this.userDetailsService.loadUserByUsername(workerUserName);
      if (userDetails != null && userDetails.isEnabled()) {
        for (final GrantedAuthority authority : userDetails.getAuthorities()) {
          if ("ROLE_WORKER".equals(authority.getAuthority())) {
            final String password = userDetails.getPassword();
            final String path = HttpServletUtils.getOriginatingRequestUri(request);
            final String time = request.getParameter("workerTime");
            final String signature = request.getParameter("workerSignature");
            final String calculatedSignature = SignatureUtil.sign(password, path, time);
            if (calculatedSignature.equals(signature)) {
              filterChain.doFilter(request, response);
            } else {
              response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
          }
        }
      }
    } catch (final Throwable e) {
    }
    response.sendError(HttpServletResponse.SC_FORBIDDEN);
  }
}
