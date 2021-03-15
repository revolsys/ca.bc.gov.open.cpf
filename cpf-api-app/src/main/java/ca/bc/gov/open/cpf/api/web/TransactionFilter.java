package ca.bc.gov.open.cpf.api.web;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jeometry.common.exception.Exceptions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.GenericFilterBean;

import com.revolsys.transaction.Propagation;
import com.revolsys.transaction.Transaction;
import com.revolsys.transaction.Transactionable;
import com.revolsys.ui.web.servlet.HttpServletLogUtil;
import com.revolsys.ui.web.utils.HttpSavedRequestAndResponse;

public class TransactionFilter extends GenericFilterBean implements Transactionable {
  private WebApplicationContext applicationContext;

  private PlatformTransactionManager transactionManager;

  @Override
  public void destroy() {
    this.applicationContext = null;
  }

  @Override
  public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
    final FilterChain filterChain) throws IOException, ServletException {
    try (
      HttpSavedRequestAndResponse saved = new HttpSavedRequestAndResponse(servletRequest,
        servletResponse);
      Transaction transaction = newTransaction(Propagation.REQUIRED);) {
      try {
        filterChain.doFilter(servletRequest, servletResponse);
      } catch (final Throwable e) {
        transaction.setRollbackOnly();
        throw e;
      }
    } catch (IOException | ServletException | Error | RuntimeException e) {
      HttpServletLogUtil.logRequestException(this, servletRequest, e);
      throw e;
    } catch (final Throwable e) {
      HttpServletLogUtil.logRequestException(this, servletRequest, e);
      throw Exceptions.wrap(e);
    }
  }

  @Override
  public PlatformTransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  @Override
  protected void initFilterBean() throws ServletException {
    final ServletContext servletContext = getServletContext();
    this.applicationContext = WebApplicationContextUtils
      .getRequiredWebApplicationContext(servletContext);
    this.transactionManager = (PlatformTransactionManager)this.applicationContext
      .getBean("transactionManager");
  }
}
