import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadersAndWriters {

    private static final int TOTAL_ESCRITORES_E_LEITORES = 100;

    public static void main(String[] args) {
        int qtdLeitores = 0;
        int qtdExecucoes = 50;

        for (int i = 0; i <= TOTAL_ESCRITORES_E_LEITORES; i++) {
            int qtdEscritores = TOTAL_ESCRITORES_E_LEITORES - qtdLeitores;
            long media = 0;

            for (int j = 0; j < qtdExecucoes; j++) {
                media += relatorioExecucaoSistema(qtdLeitores, qtdEscritores);
            }

            media /= qtdExecucoes;

            System.out.printf("Média de tempo de execução para %d leitores e %d escritores: %dms. %n", qtdLeitores, qtdEscritores, media);

            qtdLeitores++;
        }
    }

    public static long relatorioExecucaoSistema(int qtdLeitores, int qtdEscritores) {
        if ((qtdEscritores + qtdLeitores) != TOTAL_ESCRITORES_E_LEITORES) {
            throw new IllegalArgumentException("O número total de Leitores e Escritores deve ser " + TOTAL_ESCRITORES_E_LEITORES);
        }

        String nomeArquivo = "bd.txt";
        List<String> palavras = lerPalavrasDoArquivo(nomeArquivo);
        BaseDeDados base = new BaseDeDados(palavras);
        List<Runnable> threads = new ArrayList<>();

        int i = 0;

        while (++i < qtdLeitores) {
            threads.add(new Leitor(base));
        }

        while (++i < qtdEscritores) {
            threads.add(new Escritor(base));
        }

        Collections.shuffle(threads);

        long tempoInicio = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newFixedThreadPool(TOTAL_ESCRITORES_E_LEITORES)) {
            threads.forEach(executor::submit);
        }

        long tempoFim = System.currentTimeMillis();

        return tempoFim - tempoInicio;
    }

    private static List<String> lerPalavrasDoArquivo(String nomeArquivo) {
        List<String> palavras = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(nomeArquivo))) {
            String linha;

            while ((linha = br.readLine()) != null) {
                palavras.add(linha);
            }
        } catch (IOException e) {
            System.err.printf("Arquivo '%s' não encontrado. Certifique-se de que o arquivo esteja " +
                    "no mesmo diretório da classe.%n", nomeArquivo);
            System.out.println("Erro: " + e.getMessage());
        }

        return palavras;
    }
}

class BaseDeDados {

    private final List<String> palavras = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public BaseDeDados(List<String> palavras) {
        this.palavras.addAll(palavras);
    }

    // Retorna uma palavra aleatória da base
    public String lerPalavra() {
        Lock readLock = lock.readLock();
        readLock.lock();

        try {
            return palavras.get(ThreadLocalRandom.current().nextInt(palavras.size()));
        } finally {
            readLock.unlock();
        }
    }

    public void escreverPalavra() {
        Lock writeLock = lock.writeLock();
        writeLock.lock();

        try {
            int index = ThreadLocalRandom.current().nextInt(palavras.size());
            palavras.set(index, "MODIFICADO");
        } finally {
            writeLock.unlock();
        }
    }
}

class Leitor extends Thread {

    private final BaseDeDados base;

    public Leitor(BaseDeDados base) {
        this.base = base;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            String palavra = base.lerPalavra();
        }

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Escritor extends Thread {

    private final BaseDeDados base;

    public Escritor(BaseDeDados base) {
        this.base = base;
    }

    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            base.escreverPalavra();
        }

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
