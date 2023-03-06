int fact(int n, int j) {
  int x;
  if (n <= 1) return 1;
  return n * fact(n-1,2);
}
int main() {
  return fact(1,2);
}