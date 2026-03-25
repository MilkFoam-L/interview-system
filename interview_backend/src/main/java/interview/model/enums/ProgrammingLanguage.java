package interview.model.enums;

/**
 * 编程语言枚举
 * 定义支持的编程语言类型
 */
public enum ProgrammingLanguage {
    
    // 常用编程语言
    JAVA("java", "Java", ".java", "javac", "java"),
    PYTHON("python", "Python", ".py", "python", "python"),
    CPP("cpp", "C++", ".cpp", "g++", "./a.out"),
    C("c", "C", ".c", "gcc", "./a.out"),
    
    // 现代编程语言
    GO("go", "Go", ".go", "go build", "./main"),
    RUST("rust", "Rust", ".rs", "rustc", "./main"),
    
    // 脚本语言
    JAVASCRIPT("javascript", "JavaScript", ".js", "node", "node"),
    TYPESCRIPT("typescript", "TypeScript", ".ts", "tsc && node", "node"),
    PHP("php", "PHP", ".php", "php", "php"),
    RUBY("ruby", "Ruby", ".rb", "ruby", "ruby"),
    
    // 其他语言
    CSHARP("csharp", "C#", ".cs", "csc", "mono"),
    SWIFT("swift", "Swift", ".swift", "swiftc", "./main"),
    KOTLIN("kotlin", "Kotlin", ".kt", "kotlinc", "kotlin"),
    SCALA("scala", "Scala", ".scala", "scalac", "scala"),
    R("r", "R", ".r", "Rscript", "Rscript"),
    SQL("sql", "SQL", ".sql", "", ""),
    
    // 配置和脚本语言
    SHELL("shell", "Shell", ".sh", "", "bash"),
    DOCKERFILE("dockerfile", "Dockerfile", "Dockerfile", "", "docker build"),
    YAML("yaml", "YAML", ".yaml", "", ""),
    
    // 测试框架
    JUNIT("junit", "JUnit", ".java", "javac -cp .:junit-platform-console-standalone-1.8.2.jar", "java -cp .:junit-platform-console-standalone-1.8.2.jar org.junit.platform.console.ConsoleLauncher");
    
    private final String code;
    private final String displayName;
    private final String fileExtension;
    private final String compileCommand;
    private final String runCommand;
    
    ProgrammingLanguage(String code, String displayName, String fileExtension, 
                       String compileCommand, String runCommand) {
        this.code = code;
        this.displayName = displayName;
        this.fileExtension = fileExtension;
        this.compileCommand = compileCommand;
        this.runCommand = runCommand;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getFileExtension() {
        return fileExtension;
    }
    
    public String getCompileCommand() {
        return compileCommand;
    }
    
    public String getRunCommand() {
        return runCommand;
    }
    
    /**
     * 根据代码字符串获取编程语言枚举
     */
    public static ProgrammingLanguage fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        // 标准化输入
        code = code.toLowerCase().trim();
        
        // 处理别名
        switch (code) {
            case "js":
                return JAVASCRIPT;
            case "ts":
                return TYPESCRIPT;
            case "c++":
                return CPP;
            case "c#":
                return CSHARP;
            case "yml":
                return YAML;
            case "sh":
            case "bash":
                return SHELL;
            case "test":
            case "unittest":
            case "java-test":
                return JUNIT;
            default:
                break;
        }
        
        // 精确匹配
        for (ProgrammingLanguage lang : values()) {
            if (lang.getCode().equals(code)) {
                return lang;
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否为编译型语言
     */
    public boolean isCompiled() {
        return !compileCommand.isEmpty() && 
               !this.equals(PYTHON) && 
               !this.equals(JAVASCRIPT) && 
               !this.equals(PHP) && 
               !this.equals(RUBY) && 
               !this.equals(R) && 
               !this.equals(SQL) && 
               !this.equals(SHELL) && 
               !this.equals(DOCKERFILE) &&
               !this.equals(YAML);
    }
    
    /**
     * 检查是否支持该语言
     */
    public static boolean isSupported(String code) {
        return fromCode(code) != null;
    }
}
