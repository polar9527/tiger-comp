package system;

import ast.Ast;
import ast.PrettyPrintVisitor;
import ast.optimizations.Main;
import elaborator.ElaboratorVisitor;
import javacc.ParseException;
import javacc.Parser;
import junit.framework.Assert;
import org.junit.Test;

import java.io.*;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;

/**
 * Created by qc1iu on 22/03/16.
 */
public class AstOptimizationTest
{
  @Test
  public void testAll() throws IOException
  {
    try {
      // mkdir
      Process mkdir = Runtime.getRuntime().exec("mkdir build/tmp/t");
      BufferedReader mkdir_br = new BufferedReader(
          new InputStreamReader(mkdir.getInputStream()));
      while (mkdir_br.readLine() != null) {
      }
      mkdir_br = new BufferedReader(
          new InputStreamReader(mkdir.getErrorStream()));
      assertNull(mkdir_br.readLine());

      for (int i = 0; i < Result.R.length; i++) {
        Result.R r = Result.R[i];
        System.out.println("test " + r.fname);

        InputStream in = new BufferedInputStream(
            new FileInputStream("src/test/resources/" + r.fname + ".java"));
        Parser p = new Parser(in);
        Ast.Program.T prog = null;
        try {
          prog = p.parser();
        } catch (ParseException e) {
          System.err.println("Error in parser. Need debug the javacc.");
          e.printStackTrace();
        }
        // ast opt
        Main opt_main = new Main(prog);
        prog = opt_main.opt();

        PrettyPrintVisitor pp = new PrettyPrintVisitor();
        prog.accept(pp);
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream("build/tmp/t/" + r.fname + ".java")));
        w.write(pp.toString());
        w.flush();
        w.close();
        Process compile = Runtime.getRuntime()
            .exec("javac build/tmp/t/" + r.fname + ".java");
        BufferedReader br = new BufferedReader(
            new InputStreamReader(compile.getInputStream()));
        assertNull(br.readLine());
        System.out.println("  compile finished.");

        Process exec = Runtime.getRuntime()
            .exec("java -cp build/tmp/t " + r.fname);
        br = new BufferedReader(new InputStreamReader(exec.getErrorStream()));
        for (String err = br.readLine(); err != null; err = br.readLine()) {
          System.out.println(err);
        }
        br = new BufferedReader(new InputStreamReader(exec.getInputStream()));
        String[] rr = r.r;
        for (int j = 0; j < rr.length; j++) {
          assertEquals(rr[j], br.readLine());
        }
        try {
          exec.waitFor();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        System.out.println("  exec finished");
      }
    } finally {
      Process p = Runtime.getRuntime().exec("rm -rf build/tmp/t");
      BufferedReader rm_br = new BufferedReader(
          new InputStreamReader(p.getInputStream()));
      while (rm_br.readLine() != null) {
      }
      new BufferedReader(
          new InputStreamReader(p.getErrorStream()));
      while (rm_br.readLine() != null) {
      }
      System.out.println("  clean finished");
    }
  }

  @Test
  public void testOptMain()
  {
    try {
      InputStream in = new BufferedInputStream(
          new FileInputStream("src/test/resources/TestAstOptMain.java"));
      Parser p = new Parser(in);
      ast.Ast.Program.T prog = null;
      try {
        prog = p.parser();
      } catch (ParseException e) {
        e.printStackTrace();
      }
      System.out.println("  Parse finished.");
      ElaboratorVisitor ev = new ElaboratorVisitor();
      prog.accept(ev);
      assertEquals(0, ev.getErrorStack().size());
      System.out.println("  Elaborate finished.");

      //ast opt
      Main opt_main = new Main(prog);
      prog = opt_main.opt();

      assertNotNull(prog);
      Ast.Program.ProgramSingle ps = (Ast.Program.ProgramSingle) prog;
      assertEquals(1, ps.classes.size());
      Ast.Class.ClassSingle cs = (Ast.Class.ClassSingle) ps.classes.getFirst();
      assertEquals("Fac", cs.id);
      assertEquals(1, cs.methods.size());
      Ast.Method.MethodSingle ms =
          (Ast.Method.MethodSingle) cs.methods.getFirst();
      assertEquals(0, ms.stms.size());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
    }
  }
}