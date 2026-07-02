package al.unyt.mashtruesi.config;

import al.unyt.mashtruesi.model.Category;
import al.unyt.mashtruesi.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            log.info("Katalogu ekziston tashme; po anashkalohet mbushja fillestare.");
            return;
        }

        seed("Kafshe", new String[][]{
                {"Macja", "Kafshe shtepiake"},
                {"Qeni", "Mik besnik"},
                {"Luani", "Mbret i xhungles"},
                {"Elefanti", "Shume i madh"},
                {"Gjirafa", "Qafe e gjate"},
                {"Peshku", "Jeton ne uje"},
                {"Shqiponja", "Fluturon larte"},
                {"Ujku", "Bertet naten"},
                {"Dhelpra", "Dinake"},
                {"Ariu", "Fle ne dimer"},
                {"Lepuri", "Veshe te gjata"},
                {"Kali", "Vrapon shpejt"}
        });

        seed("Ushqime", new String[][]{
                {"Byrek", "Me petulla"},
                {"Pica", "E rrumbullaket"},
                {"Buke", "E perditshme"},
                {"Djathe", "Nga qumeshti"},
                {"Molle", "Fruta"},
                {"Banane", "E verdhe"},
                {"Supe", "E ngrohte"},
                {"Sallate", "E fresket"},
                {"Akullore", "E ftohte"},
                {"Cokollate", "E embel"},
                {"Kafe", "Te zgjon"},
                {"Mish", "Me proteina"}
        });

        seed("Sporte", new String[][]{
                {"Futboll", "Me top"},
                {"Basketboll", "Me kosh"},
                {"Tenis", "Me raketa"},
                {"Not", "Ne uje"},
                {"Vrapim", "Me kembe"},
                {"Boks", "Me dorashka"},
                {"Volejboll", "Me rrjete"},
                {"Ciklizem", "Me biciklete"},
                {"Ski", "Ne bore"},
                {"Shah", "Mendor"}
        });

        seed("Vende", new String[][]{
                {"Tirane", "Kryeqytet"},
                {"Durres", "Buze detit"},
                {"Shkoder", "Ne veri"},
                {"Plazh", "Me rere"},
                {"Mal", "I larte"},
                {"Spital", "Per te semuret"},
                {"Shkolle", "Per nxenesit"},
                {"Aeroport", "Per avionet"},
                {"Treg", "Per pazar"},
                {"Park", "I gjelber"},
                {"Biblioteke", "Me libra"},
                {"Restorant", "Per te ngrene"}
        });

        seed("Profesione", new String[][]{
                {"Mjek", "Ne spital"},
                {"Mesues", "Ne shkolle"},
                {"Inxhinier", "Ndertimi"},
                {"Polic", "Ruan rendin"},
                {"Kuzhinier", "Gatuan"},
                {"Avokat", "Ne gjykate"},
                {"Piktor", "Me ngjyra"},
                {"Shofer", "Drejton makinen"},
                {"Zjarrfikes", "Fik zjarrin"},
                {"Programues", "Me kompjuter"}
        });

        seed("Objekte Shtepie", new String[][]{
                {"Karrige", "Per t'u ulur"},
                {"Tavoline", "E sheshte"},
                {"Frigorifer", "Mban te ftohte"},
                {"Televizor", "Me ekran"},
                {"Llambe", "Ben drite"},
                {"Krevat", "Per te fjetur"},
                {"Pasqyre", "Reflekton"},
                {"Ore", "Tregon kohen"},
                {"Celes", "Hap deren"},
                {"Telefon", "Per te folur"}
        });

        long categories = categoryRepository.count();
        log.info("Mbushja fillestare perfundoi: {} kategori.", categories);
    }

    private void seed(String name, String[][] words) {
        Category category = new Category(name);
        for (String[] pair : words) {
            category.addWord(pair[0], pair[1]);
        }
        categoryRepository.save(category);
    }
}
