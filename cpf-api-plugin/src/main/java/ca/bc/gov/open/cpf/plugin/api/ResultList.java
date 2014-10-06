package ca.bc.gov.open.cpf.plugin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * <p>The <code>ResultList</code> annotation can be used in <a href="../../structuredData.html">structured result data</a>
 * plug-ins that return a list of structured results instead of a single result. For example a geocoder that
 * returns a list of potential matches.</p>
 * 
 * <p>To return a list of results the plug-in must implement a get method that returns a {@link List} of a
 * specified value object (e.g. <code>ResultObject</code>). The method must also have the
 * {@link ResultList} annotation. The plug-in class must not have any methods marked with the {@link ResultAttribute} annotation.</p>
 * 
 * <p>The value object class contains the result attributes to return for each result in the list.
 * Pick an appropriate name for the value object (e.g. Address). The value object class must have
 * one or more get methods with the {@link ResultAttribute} annotation. These
 * are the attributes that are returned to the client. The same rules apply as per structured result attributes.</p>
 * 
 * <p>The following example shows the use of the annotation on a {@link BusinessApplicationPlugin} method.</p>

<figure>
<pre class="prettyprint">private List&lt;ResultObject&gt; 

&#064;ResultList
public List&lt;ResultObject&gt; getResults() {
  return results;
}</pre>
</figure>

 * <p>The following example shows the value object implementation for the above example.</p>

<figure>
<pre class="prettyprint">public class ResultObject
private String attribute1;

&#064;ResultAttribute
public int getField1 () {
  return attribute1;
}</pre>
</figure>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface ResultList {
}
