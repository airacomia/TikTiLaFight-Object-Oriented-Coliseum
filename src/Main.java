import java.util.Scanner;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== PICK YOUR ROOSTER ===");
        System.out.println("1. Manok na Puti (Balanced)");
        System.out.println("2. Manok na Itim (High Attack)");
        System.out.println("3. Manok na Bato (High Defense)");
        System.out.print("Choose your rooster: ");
        int playerChoice = scanner.nextInt();

        Rooster player;
        switch (playerChoice) {
        case 1:
            player = new Rooster.ManokNaPuti("Red Rooster");
            break;
        case 2:
            player = new Rooster.ManokNaItim("Red Rooster");
            break;
        case 3:
            player = new Rooster.ManokNaBato("Red Rooster");
            break;
        default:
            System.out.println("Invalid choice! Defaulting to Manok na Puti.");
            player = new Rooster.ManokNaPuti("Red Rooster");
            break;
        }
        int enemyChoice = 1 + (int) (Math.random() * 3);
        Rooster enemy;
        switch (enemyChoice) {
        case 1:
            enemy = new Rooster.ManokNaPuti("Blue Rooster");
            break;
        case 2:
            enemy = new Rooster.ManokNaItim("Blue Rooster");
            break;
        case 3:
            enemy = new Rooster.ManokNaBato("Blue Rooster");
            break;
        default:
             enemy = new Rooster.ManokNaPuti("Blue Rooster");
             break;
        }
        System.out.println("=== ROOSTER BATTLE ===");
        System.out.println(player.getName() + " vs " + enemy.getName());
        System.out.println();


        while (!player.isFainted() && !enemy.isFainted()) {

            System.out.println("\n--- " + player.getName() + "'s Turn ---");
            System.out.println("HP: " + player.getHp());
            System.out.println("Choose your skill:");

            List<Skill> playerSkills = player.getSkills();
            for (int i = 0; i < playerSkills.size(); i++) {
                Skill skill = playerSkills.get(i);
                System.out.println((i + 1) + ". " + skill.getName() +
                        " (Damage: " + skill.getDamage() +
                        ", Type: " + skill.getType() + ")");
            }

            int choice = scanner.nextInt() - 1;
            if (choice >= 0 && choice < playerSkills.size()) {
                player.attack(enemy, playerSkills.get(choice));
            } else {
                System.out.println("Invalid choice! Using basic attack.");
                player.attack(enemy);
            }

            if (enemy.isFainted()) {
                System.out.println("\n" + enemy.getName() + " has fainted!");
                System.out.println(player.getName() + " wins!");
                break;
            }


            System.out.println("\n--- " + enemy.getName() + "'s Turn ---");
            System.out.println("HP: " + enemy.getHp());

            List<Skill> enemySkills = enemy.getSkills();
            int randomSkill = (int) (Math.random() * enemySkills.size());
            enemy.attack(player, enemySkills.get(randomSkill));

            if (player.isFainted()) {
                System.out.println("\n" + player.getName() + " has fainted!");
                System.out.println(enemy.getName() + " wins!");
                break;
            }
        }

        scanner.close();
        System.out.println("\n=== BATTLE END ===");
    }
}
