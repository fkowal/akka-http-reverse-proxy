# Akka Http Reverse Proxy

## Usage

Start ReverseProxy service running on port 9000 with sbt:

```
$ sbt
> ~re-start
```

in second terminal run DemoApp on port 9001
```
sbt run
```

With the service up, you can start sending HTTP requests:

Directly to DemoService
```
$ curl http://localhost:9001/api/person/maciej
{
  "city": "maciej",
  "age": 123
}
```
Or via Reverse Proxy
```
$ curl http://localhost:9000/service2/api/person/maciej
```


### Testing

Execute tests using `test` command:

```
$ sbt
> test
```

## Author & license

Maciej Kowalski

For licensing info see LICENSE file in project's root directory.
