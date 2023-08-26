# Pathom Demo Server

A simple pathom backed API based on the official [Pathom Tutorial](https://pathom3.wsscode.com/docs/tutorial)

The API accepts EQL queries as JSON. Since there is no standard way of converting from JSON->EQL, I've decided to simply convert all keyword-like string values as keywords. 

IE 

``` javascript
{
    ":pathom/entity":{":ip":"192.29.213.3"},
    ":pathom/eql":[":state", ":city", ":temperature"]
}
```

Will be parsed as

``` clojure
{
    :pathom/entity {:ip "192.29.213.3"}, 
    :pathom/eql [:state :city :temperature]
}
```

User `./run-server.sh` to run a dev server

To test, run 

```bash
curl --location 'localhost:3000/api' \
--header 'Content-Type: application/json' \
--data '{":pathom/entity":{":ip":"192.29.213.3"},":pathom/eql":[":state", ":city", ":temperature"]}'
```

