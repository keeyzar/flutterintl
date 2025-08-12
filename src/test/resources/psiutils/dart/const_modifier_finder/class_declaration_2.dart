void main() {
  const Outer(Hello("Hello world"));
}

class Hello{
  final String text;
  const Hello(this.text);
}

class Outer{
  final Hello hello;
  const Outer(this.hello);
}
