## Resource Description
The resource description file format is used to describe a resource and any child resources that
are children of the resource.

The document contains a single <code>resource</code> object with the following fields.

<div class="table-responsive">
<table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Attribute</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>resourceUri</td>
      <td>The URI to the resource.</td>
    </tr>
    <tr>
      <td>title</td>
      <td>The title of the resource.</td>
    </tr>
    <tr>
      <td>resources</td>
      <td>An array of <code>resource</code> objects that can be accessed from this resource.</td>
    </tr>
    <tr>
      <td><i>&lt;customAttribute&gt;</i></td>
      <td>A custom attribute specific to that resource.</td>
    </tr>
  </tbody>
</table>
</div>

See the links below for example resource list documents.

* [JSON](ws/apps.json)
* [XML](ws/apps.xml)

