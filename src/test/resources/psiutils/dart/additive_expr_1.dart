void main() {
  String cool = "3";
  var omg = Nice();
  String complexString = """
    hello ${cool} world $cool ${omg.another} 
  """;
}
class Nice {
  String another = "lol";
}
