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
package ca.bc.gov.open.cpf.plugins.mandelbrot;

import org.jeometry.common.number.Doubles;

public class Complex {
  public static Complex plus(final Complex a, final Complex b) {
    final double real = a.real + b.real;
    final double imaginary = a.imaginary + b.imaginary;
    final Complex sum = new Complex(real, imaginary);
    return sum;
  }

  private final double real;

  private final double imaginary;

  public Complex(final double real, final double imaginary) {
    this.real = real;
    this.imaginary = imaginary;
  }

  public double abs() {
    return Math.hypot(this.real, this.imaginary);
  }

  public Complex conjugate() {
    return new Complex(this.real, -this.imaginary);
  }

  public Complex cos() {
    return new Complex(Math.cos(this.real) * Math.cosh(this.imaginary),
      -Math.sin(this.real) * Math.sinh(this.imaginary));
  }

  public Complex divides(final Complex b) {
    final Complex a = this;
    return a.times(b.reciprocal());
  }

  public Complex exp() {
    return new Complex(Math.exp(this.real) * Math.cos(this.imaginary),
      Math.exp(this.real) * Math.sin(this.imaginary));
  }

  public double getImaginary() {
    return this.imaginary;
  }

  public double getReal() {
    return this.real;
  }

  public Complex minus(final Complex b) {
    final Complex a = this;
    final double real = a.real - b.real;
    final double imaginary = a.imaginary - b.imaginary;
    return new Complex(real, imaginary);
  }

  public double phase() {
    return Math.atan2(this.imaginary, this.real);
  }

  public Complex plus(final Complex b) {
    final Complex a = this; // invoking object
    final double real = a.real + b.real;
    final double imaginary = a.imaginary + b.imaginary;
    return new Complex(real, imaginary);
  }

  public Complex reciprocal() {
    final double scale = this.real * this.real + this.imaginary * this.imaginary;
    return new Complex(this.real / scale, -this.imaginary / scale);
  }

  public Complex sin() {
    return new Complex(Math.sin(this.real) * Math.cosh(this.imaginary),
      Math.cos(this.real) * Math.sinh(this.imaginary));
  }

  public Complex tan() {
    return sin().divides(cos());
  }

  public Complex times(final Complex b) {
    final double real = this.real * b.real - this.imaginary * b.imaginary;
    final double imaginary = this.real * b.imaginary + this.imaginary * b.real;
    return new Complex(real, imaginary);
  }

  public Complex times(final double alpha) {
    return new Complex(alpha * this.real, alpha * this.imaginary);
  }

  @Override
  public String toString() {
    if (this.imaginary == 0) {
      return Doubles.toString(this.real);
    } else if (this.real == 0) {
      return Doubles.toString(this.imaginary) + "i";
    } else if (this.imaginary < 0) {
      return this.real + " - " + Doubles.toString(-this.imaginary) + "i";
    } else {
      return this.real + " + " + Doubles.toString(this.imaginary) + "i";
    }
  }
}
