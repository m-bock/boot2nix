[
{{#dependencies}}
  {
    repoUrl = "{{repo-url}}";
    subDir = "{{sub-dir}}";
    {{#jar}}
    jar = {
      file = "{{file}}";
      sha1 = "{{sha1}}";
    };
    {{/jar}}
    {{#pom}}
    pom = {
      file = "{{file}}";
      sha1 = "{{sha1}}";
    };
    {{/pom}}
  }
{{/dependencies}}
]
