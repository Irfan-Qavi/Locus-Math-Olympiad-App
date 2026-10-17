package com.locus.olympiad.data

import android.content.Context
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.TheoryModule
import com.locus.olympiad.data.model.UserTopicProfile

/**
 * Pre-populates the Room database with standard Elo profiles, rich 3-tier theory monographs,
 * and competitive math olympiad seed questions.
 */
object DatabaseInitializer {

    suspend fun seedDatabase(context: Context?, database: LocusDatabase) {
        val profileDao = database.userTopicProfileDao()
        val questionDao = database.practiceQuestionDao()
        val theoryDao = database.theoryModuleDao()

        // 1. Seed default UserTopicProfile for each MathTopic
        if (profileDao.countProfiles() == 0) {
            val initialProfiles = MathTopic.values().map { topic ->
                UserTopicProfile(
                    category = topic.displayName,
                    eloRating = UserTopicProfile.DEFAULT_ELO // 1000
                )
            }
            profileDao.insertProfiles(initialProfiles)
        }

        // 2. Seed practice questions if table is empty
        if (questionDao.countQuestions() == 0) {
            val parsedQuestions = if (context != null) {
                SeedDataParser.parseSeedQuestions(context)
            } else {
                emptyList()
            }

            if (parsedQuestions.isNotEmpty()) {
                questionDao.insertQuestions(parsedQuestions)
            } else {
                questionDao.insertQuestions(getDefaultSampleQuestions())
            }
        }

        // 3. Seed Theory Modules if table is empty
        if (theoryDao.countModules() == 0) {
            theoryDao.insertModules(getDefaultSampleModules())
        }
    }

    private fun getDefaultSampleQuestions(): List<PracticeQuestion> {
        return listOf(
            PracticeQuestion(
                category = "Combinatorics",
                difficultyElo = 1000,
                questionType = "OBJECTIVE",
                latexContent = """Let ${'$'}G = (V, E)${'$'} be a connected simple graph with ${'$'}n \ge 4${'$'} vertices. Suppose for every partition ${'$'}V = A \cup B${'$'} of the vertex set into non-empty disjoint subsets, the number of crossing edges satisfies:
${'$'}${'$'}e(A, B) \ge \left\lceil \frac{|A| \cdot |B|}{n - 1} \right\rceil + \sum_{v \in A} \operatorname{deg}(v) \pmod 2${'$'}${'$'}
Determine the minimum possible chromatic number ${'$'}\chi(G)${'$'} across all such graphs.""",
                correctAnswer = "3",
                listOfHints = listOf(
                    """Consider the parity of the degree sum over ${'$'}A${'$'} and examine the case where ${'$'}G${'$'} contains an Eulerian sub-graph.""",
                    """If ${'$'}\chi(G) = 2${'$'}, then ${'$'}G${'$'} is bipartite. Partition ${'$'}V${'$'} into the two color classes and check whether the parity constraint can hold for all cuts.""",
                    """Notice that a cycle ${'$'}C_5${'$'} or ${'$'}K_3${'$'} requires at least 3 colors. Test if ${'$'}\chi(G)=3${'$'} satisfies the lower bound by constructing an admissible 3-chromatic triangulation."""
                ),
                source = "International Olympiad Training • Tier III",
                tier = "Tier III"
            ),
            PracticeQuestion(
                category = "Combinatorics",
                difficultyElo = 1080,
                questionType = "OBJECTIVE",
                latexContent = """Find the number of permutations ${'$'}\pi${'$'} of ${'$'}\{1, 2, \dots, 6\}${'$'} such that for all ${'$'}1 \le k \le 5${'$'}:
${'$'}${'$'}|\pi(k) - \pi(k+1)| \ne 1${'$'}${'$'}""",
                correctAnswer = "90",
                listOfHints = listOf(
                    """Use the Principle of Inclusion-Exclusion (PIE) on the conditions that adjacent elements are consecutive.""",
                    """Let ${'$'}A_i${'$'} be the property that ${'$'}|\pi(i) - \pi(i+1)| = 1${'$'}. Count the intersections by grouping adjacent pairs into blocks.""",
                    """Evaluate ${'$'}\sum_{k=0}^5 (-1)^k N(k)${'$'} using the number of ways to place ${'$'}k${'$'} disjoint domino tiles."""
                ),
                source = "IOQM Final Stage",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Algebra",
                difficultyElo = 1020,
                questionType = "OBJECTIVE",
                latexContent = """Find all real solutions ${'$'}(x, y)${'$'} to the symmetric system of equations:
${'$'}${'$'}\begin{cases} x^3 - y^3 = 7 \\ x^2 y - x y^2 = 2 \end{cases}${'$'}${'$'}
What is the maximum value of ${'$'}x + y${'$'}?""",
                correctAnswer = "3",
                listOfHints = listOf(
                    """Notice that ${'$'}x^3 - 3x^2y + 3xy^2 - y^3 = (x - y)^3${'$'}.""",
                    """Compute ${'$'}(x - y)^3 = (x^3 - y^3) - 3(x^2y - xy^2) = 7 - 3(2) = 1${'$'}. Thus ${'$'}x - y = 1${'$'}.""",
                    """Substitute ${'$'}x = y + 1${'$'} into ${'$'}xy(x - y) = 2${'$'} to find ${'$'}y(y + 1) = 2${'$'}, giving roots ${'$'}y = 1${'$'} or ${'$'}y = -2${'$'}."""
                ),
                source = "RMO Algebra Round",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Algebra",
                difficultyElo = 1140,
                questionType = "SUBJECTIVE_SHORT",
                latexContent = """Let ${'$'}P(x)${'$'} be a polynomial with integer coefficients such that ${'$'}P(1) = 2024${'$'} and ${'$'}P(2024) = 1${'$'}. 
If ${'$'}P(k) = k${'$'} for some integer ${'$'}k${'$'}, find the value of ${'$'}k${'$'} (Enter 'No solution' if none exists).""",
                correctAnswer = "No solution",
                listOfHints = listOf(
                    """Recall the fundamental integer polynomial divisibility property: ${'$'}(a - b) \mid (P(a) - P(b))${'$'} for distinct integers ${'$'}a, b${'$'}.""",
                    """Apply this property to pairs ${'$'}(k, 1)${'$'} and ${'$'}(k, 2024)${'$'}.""",
                    """Analyze whether ${'$'}(k - 1) \mid (k - 2024)${'$'} and ${'$'}(k - 2024) \mid (k - 1)${'$'} can both hold simultaneously."""
                ),
                source = "IOQM National Camp",
                tier = "Tier III"
            ),
            PracticeQuestion(
                category = "Algebra",
                difficultyElo = 1450,
                questionType = "SUBJECTIVE_PROOF",
                latexContent = """Prove that for all positive real numbers ${'$'}a, b, c${'$'}:
${'$'}${'$'}\frac{a}{b + c} + \frac{b}{c + a} + \frac{c}{a + b} \ge \frac{3}{2}${'$'}${'$'}""",
                correctAnswer = "Nesbitt Inequality holds by Cauchy-Schwarz",
                proofSolution = """By Cauchy-Schwarz in Engel form (Sedrakyan lemma):
${'$'}${'$'}\sum_{cyc} \frac{a}{b+c} = \sum_{cyc} \frac{a^2}{ab + ac} \ge \frac{(a+b+c)^2}{2(ab+bc+ca)}${'$'}${'$'}
Since ${'$'}(a+b+c)^2 \ge 3(ab+bc+ca)${'$'}, we have:
${'$'}${'$'}\frac{(a+b+c)^2}{2(ab+bc+ca)} \ge \frac{3}{2}${'$'}${'$'}
Equality holds if and only if ${'$'}a = b = c${'$'}.""",
                listOfHints = listOf(
                    """Consider rewriting each term by adding 1: ${'$'}\frac{a}{b+c} + 1 = \frac{a+b+c}{b+c}${'$'}.""",
                    """Apply Cauchy-Schwarz inequality or AM-HM inequality to ${'$'}\sum \frac{1}{b+c}${'$'}.""",
                    """Alternatively, apply Sedrakyan's lemma (Cauchy-Schwarz in Engel form)."""
                ),
                source = "Nesbitt's Classical Lemma • Tier II",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Number Theory",
                difficultyElo = 990,
                questionType = "OBJECTIVE",
                latexContent = """Find the remainder when ${'$'}2^{2026} + 3^{2026}${'$'} is divided by ${'$'}13${'$'}.""",
                correctAnswer = "0",
                listOfHints = listOf(
                    """Apply Fermat's Little Theorem: for prime ${'$'}p=13${'$'} and ${'$'}\gcd(a, 13) = 1${'$'}, ${'$'}a^{12} \equiv 1 \pmod{13}${'$'}.""",
                    """Reduce the exponent ${'$'}2026${'$'} modulo ${'$'}12${'$'}: ${'$'}2026 = 12 \times 168 + 10${'$'}.""",
                    """Calculate ${'$'}2^{10} \pmod{13}${'$'} and ${'$'}3^{10} \pmod{13}${'$'} and sum them up modulo 13."""
                ),
                source = "IOQM Prelims",
                tier = "Tier I"
            ),
            PracticeQuestion(
                category = "Number Theory",
                difficultyElo = 1050,
                questionType = "OBJECTIVE",
                latexContent = """Find all pairs of primes ${'$'}(p, q)${'$'} such that:
${'$'}${'$'}p^2 - 2q^2 = 1${'$'}${'$'}
What is the sum ${'$'}p + q${'$'}?""",
                correctAnswer = "5",
                listOfHints = listOf(
                    """Rewrite the equation as ${'$'}p^2 - 1 = 2q^2${'$'}, which factors as ${'$'}(p - 1)(p + 1) = 2q^2${'$'}.""",
                    """Consider the parity of ${'$'}p${'$'}. If ${'$'}p=2${'$'}, ${'$'}4 - 2q^2 = 1${'$'} has no prime solution. Thus ${'$'}p${'$'} must be odd.""",
                    """Since ${'$'}p${'$'} is odd, both ${'$'}p - 1${'$'} and ${'$'}p + 1${'$'} are consecutive even integers. Analyze the distribution of factors of ${'$'}2q^2${'$'} to deduce ${'$'}q = 2${'$'} and ${'$'}p = 3${'$'}."""
                ),
                source = "RMO Number Theory",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Geometry",
                difficultyElo = 1010,
                questionType = "OBJECTIVE",
                latexContent = """In acute triangle ${'$'}\triangle ABC${'$'}, let ${'$'}H${'$'} be the orthocenter and ${'$'}O${'$'} the circumcenter. If ${'$'}\angle A = 60^\circ${'$'} and ${'$'}BC = 12${'$'}, compute the length of the segment ${'$'}OH${'$'}.""",
                correctAnswer = "12",
                listOfHints = listOf(
                    """Recall Euler's triangle formula or the relation between ${'$'}AH${'$'} and circumradius ${'$'}R${'$'}: ${'$'}AH = 2R \cos A${'$'}.""",
                    """For ${'$'}\angle A = 60^\circ${'$'}, ${'$'}\cos 60^\circ = 1/2${'$'}, so ${'$'}AH = R${'$'}.""",
                    """Notice that in quadrilateral ${'$'}AHCO${'$'} or by the distance formula ${'$'}OH^2 = 9R^2 - (a^2+b^2+c^2)${'$'}, what is ${'$'}R${'$'} when ${'$'}a=12${'$'} and ${'$'}\angle A=60^\circ${'$'}? ${'$'}a = 2R \sin 60^\circ${'$'}, giving ${'$'}R = 4\sqrt{3}${'$'}."""
                ),
                source = "RMO Geometry",
                tier = "Tier II"
            ),
            PracticeQuestion(
                category = "Geometry",
                difficultyElo = 1130,
                questionType = "SUBJECTIVE_SHORT",
                latexContent = """Let ${'$'}\Gamma${'$'} be the incircle of ${'$'}\triangle ABC${'$'} tangent to ${'$'}BC, CA, AB${'$'} at ${'$'}D, E, F${'$'} respectively. If ${'$'}AB = 13${'$'}, ${'$'}BC = 14${'$'}, ${'$'}CA = 15${'$'}, compute the area of ${'$'}\triangle DEF${'$'}.""",
                correctAnswer = "84/5",
                listOfHints = listOf(
                    """First compute the area and inradius of ${'$'}\triangle ABC${'$'} using Heron's formula: ${'$'}s = 21, \Delta = 84, r = 4${'$'}.""",
                    """Use contact triangle properties: ${'$'}AE = AF = s - a = 7${'$'}, ${'$'}BD = BF = s - b = 8${'$'}, ${'$'}CD = CE = s - c = 6${'$'}.""",
                    """Express Area(${'$'}\triangle DEF${'$'}) in terms of ${'$'}\triangle ABC${'$'} by subtracting the outer vertex triangles or using the formula ${'$'}\frac{r \cdot \text{Area}(\triangle ABC)}{2R}${'$'}."""
                ),
                source = "IOQM National Level",
                tier = "Tier III"
            )
        )
    }

    private fun getDefaultSampleModules(): List<TheoryModule> {
        return listOf(
            TheoryModule(
                category = "Combinatorics",
                chapterName = "Chapter 1: Extremal Principles & Graph Theorems",
                chapterNumber = 1,
                title = "Alon's Combinatorial Nullstellensatz",
                readProgressPercent = 85,
                latexContent = """\section*{Alon's Combinatorial Nullstellensatz}
Let ${'$'}\mathbb{F}${'$'} be an arbitrary field, and let ${'$'}P \in \mathbb{F}[x_1, \dots, x_n]${'$'} be a polynomial of degree ${'$'}\deg(P) = \sum_{i=1}^n t_i${'$'}, where each ${'$'}t_i${'$'} is a non-negative integer.

\begin{theorem}[Noga Alon, 1999]
If the coefficient of the monomial ${'$'}\prod_{i=1}^n x_i^{t_i}${'$'} in ${'$'}P${'$'} is non-zero, and if ${'$'}S_1, \dots, S_n \subseteq \mathbb{F}${'$'} are subsets of cardinality ${'$'}|S_i| > t_i${'$'} for all ${'$'}i \in \{1, \dots, n\}${'$'}, then there exists a tuple:
${'$'}${'$'}(s_1, \dots, s_n) \in S_1 \times S_2 \times \cdots \times S_n${'$'}${'$'}
such that ${'$'}P(s_1, \dots, s_n) \ne 0${'$'}.
\end{theorem}

\subsection*{Application to Graph Coloring}
Consider the graph polynomial ${'$'}f_G(x) = \prod_{uv \in E, u < v} (x_u - x_v)${'$'}. If ${'$'}G${'$'} is ${'$'}k${'$'}-choosable, we construct non-vanishing evaluations over list colorings ${'$'}L(v)${'$'} with ${'$'}|L(v)| \ge k${'$'}.""",
                heuristicsAndTricks = """\textbf{Key Olympiad Heuristics & Mental Models:}
\begin{itemize}
    \item \textbf{Degree Matching Rule:} To apply Nullstellensatz, you must match the degree of each variable ${'$'}t_i < |S_i|${'$'}. Always expand only the leading monomial.
    \item \textbf{Look at the Extremes:} Pick vertices of maximum degree or extreme coordinate hyperplanes when defining the vanishing set.
    \item \textbf{Subsets of Restricted Sums:} For problems of type ${'$'}a + b \ne c${'$'}, write polynomial ${'$'}\prod (x_i + x_j - c)${'$'} and verify the coefficient of ${'$'}x_1^{|A|-1} x_2^{|B|-1}${'$'}.
\end{itemize}"""
            ),
            TheoryModule(
                category = "Combinatorics",
                chapterName = "Chapter 1: Extremal Principles & Graph Theorems",
                chapterNumber = 1,
                title = "Erdős-Szekeres Monotone Subsequences",
                readProgressPercent = 60,
                latexContent = """\section*{The Erdős-Szekeres Theorem}
\begin{theorem}[Erdős-Szekeres, 1935]
Every sequence of ${'$'}n = rs + 1${'$'} distinct real numbers contains either an increasing subsequence of length ${'$'}r + 1${'$'} or a decreasing subsequence of length ${'$'}s + 1${'$'}.
\end{theorem}

\subsection*{Pigeonhole Proof via Coordinates}
For each index ${'$'}k${'$'}, assign the coordinate ${'$'}(a_k, b_k)${'$'}, where ${'$'}a_k${'$'} is the length of the longest increasing subsequence ending at ${'$'}x_k${'$'}, and ${'$'}b_k${'$'} is the length of the longest decreasing subsequence ending at ${'$'}x_k${'$'}. If ${'$'}a_k \le r${'$'} and ${'$'}b_k \le s${'$'} for all ${'$'}k${'$'}, then there are at most ${'$'}rs${'$'} distinct pairs ${'$'}(a_k, b_k)${'$'}. By the Pigeonhole Principle, some pair must repeat, which easily yields a contradiction.""",
                heuristicsAndTricks = """\textbf{Competition Heuristics:}
\begin{itemize}
    \item \textbf{Dilworth's Poset Duality:} The minimum number of chains needed to cover a poset equals the maximum size of an antichain.
    \item \textbf{Grid Invariance:} When ordering elements, assign a dynamic 2D index to track growth in both orientations.
\end{itemize}"""
            ),
            TheoryModule(
                category = "Algebra",
                chapterName = "Chapter 1: Diophantine Symmetries & Polynomials",
                chapterNumber = 1,
                title = "Vieta Jumping & Minimal Descent",
                readProgressPercent = 70,
                latexContent = """\section*{Vieta's Root Flipping (Vieta Jumping)}
Vieta jumping is a proof technique in Olympiad number theory and algebra, especially effective for Diophantine equations involving symmetric quadratic forms.

\begin{theorem}
Let ${'$'}(x, y)${'$'} be a solution to ${'$'}x^2 - Axy + y^2 = B${'$'}. Fixing ${'$'}y${'$'}, the equation is quadratic in ${'$'}x${'$'}:
${'$'}${'$'}x^2 - (Ay)x + (y^2 - B) = 0${'$'}${'$'}
By Vieta's formulas, if ${'$'}x_1${'$'} is one root, the second root ${'$'}x_2 = Ay - x_1 = \frac{y^2 - B}{x_1}${'$'} yields a new integer solution.
\end{theorem}""",
                heuristicsAndTricks = """\textbf{Strategic Instincts for Vieta Jumping:}
\begin{itemize}
    \item \textbf{Minimal Counterexample:} Assume a non-square ratio ${'$'}k = \frac{x^2+y^2}{xy+1}${'$'} exists, and pick the pair ${'$'}(x, y)${'$'} that minimizes the sum ${'$'}x + y${'$'}.
    \item \textbf{Descent Contradiction:} Show that the reflected root ${'$'}x_2${'$'} is strictly positive and satisfies ${'$'}x_2 + y < x_1 + y${'$'}, contradicting minimality.
\end{itemize}"""
            ),
            TheoryModule(
                category = "Number Theory",
                chapterName = "Chapter 1: Diophantine Approximation & Hurwitz Bounds",
                chapterNumber = 1,
                title = "Dirichlet's Approximation Theorem",
                readProgressPercent = 55,
                latexContent = """\section*{Dirichlet's Approximation Theorem}
Let ${'$'}\alpha${'$'} be an arbitrary real number and let ${'$'}N \ge 1${'$'} be a positive integer.

\begin{theorem}
There exist integers ${'$'}p${'$'} and ${'$'}q${'$'} with ${'$'}1 \le q \le N${'$'} such that:
${'$'}${'$'}\left| \alpha - \frac{p}{q} \right| < \frac{1}{q N} \le \frac{1}{q^2}${'$'}${'$'}
Moreover, if ${'$'}\alpha${'$'} is irrational, there exist infinitely many pairs of coprime integers ${'$'}(p, q)${'$'} such that:
${'$'}${'$'}\left| \alpha - \frac{p}{q} \right| < \frac{1}{\sqrt{5} q^2}${'$'}${'$'}
The constant ${'$'}\sqrt{5}${'$'} is best possible (Hurwitz's Theorem, achieved by the golden ratio ${'$'}\phi${'$'}).
\end{theorem}""",
                heuristicsAndTricks = """\textbf{Competition Heuristics for Approximation:}
\begin{itemize}
    \item \textbf{Fractional Part Pigeonhole:} Partition the unit interval $[0, 1)$ into $N$ equal sub-intervals and consider the $N+1$ fractional parts $\{0\}, \{\alpha\}, \{2\alpha\}, \dots, \{N\alpha\}$.
    \item \textbf{Continued Fractions:} The best rational approximations $p_n/q_n$ are the convergents of the simple continued fraction of $\alpha$.
\end{itemize}"""
            ),
            TheoryModule(
                category = "Geometry",
                chapterName = "Chapter 1: Projective & Barycentric Configurations",
                chapterNumber = 1,
                title = "Homogeneous Barycentric Coordinates",
                readProgressPercent = 90,
                latexContent = """\section*{Homogeneous Barycentric Coordinates}
Let ${'$'}\triangle ABC${'$'} be a reference triangle. Any point ${'$'}P${'$'} in the plane can be uniquely represented up to scale by homogeneous coordinates ${'$'}(u : v : w)${'$'} where:
${'$'}${'$'}u = \frac{\operatorname{Area}(\triangle PBC)}{\operatorname{Area}(\triangle ABC)}, \quad v = \frac{\operatorname{Area}(\triangle PCA)}{\operatorname{Area}(\triangle ABC)}, \quad w = \frac{\operatorname{Area}(\triangle PAB)}{\operatorname{Area}(\triangle ABC)}${'$'}${'$'}
with ${'$'}u + v + w = 1${'$'}. The incenter has coordinates ${'$'}(a : b : c)${'$'}, and the circumcenter has coordinates ${'$'}(\sin 2A : \sin 2B : \sin 2C)${'$'}.""",
                heuristicsAndTricks = """\textbf{Strategic Shortcuts in Barycentric Geometry:}
\begin{itemize}
    \item \textbf{Area Coordinates:} Centroid is $(1:1:1)$, Incenter is $(a:b:c)$, Excenter $I_a$ is $(-a:b:c)$.
    \item \textbf{Collinearity Determinant:} Three points $P_1, P_2, P_3$ are collinear if and only if $\det(P_1, P_2, P_3) = 0$.
    \item \textbf{Concurrency of Cevians:} Ceva's Theorem in barycentrics reduces to $u_1 v_2 w_3 = u_2 v_3 w_1$.
\end{itemize}"""
            )
        )
    }
}
