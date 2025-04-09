package main

/*
#cgo CFLAGS: -I.
#cgo LDFLAGS: -L. -lfibcalculator
extern long long calculate_sum_of_fibs(int count, int n);
*/
import "C"
import (
	"fmt"
	"runtime"
	"sync"
	"time"
)

func fibGo(n int) int64 {
	if n <= 0 {
		return 0
	}
	if n == 1 {
		return 1
	}

	a, b := int64(0), int64(1)
	for i := 2; i <= n; i++ {
		temp := a + b
		a = b
		b = temp
	}
	return b
}

// calculateSumOfFibsGo same as c func, but in go
func calculateSumOfFibsGo(count int, n int) int64 {
	var totalSum int64 = 0
	for i := 0; i < count; i++ {
		totalSum += fibGo(n)
	}
	return totalSum
}

func main() {
	numGoroutines := runtime.NumCPU()
	iterationsPerGoroutine := 10000000
	fibN := 92 // max possible to prevent overflow for int64

	fmt.Printf("number of goroutines: %d\n", numGoroutines)

	startTimeCGO := time.Now()
	resultsCGO := runWithGoroutines(numGoroutines, iterationsPerGoroutine, fibN, func(count, n int) int64 {
		res := C.calculate_sum_of_fibs(C.int(count), C.int(n))
		return int64(res)
	})
	durationCGO := time.Since(startTimeCGO)
	fmt.Printf("cgo time: \t\t%v\n", durationCGO)
	fmt.Printf("cgo total sum: \t\t%d\n", sumResults(resultsCGO))
	const delimiter = "----------------------------------------"
	fmt.Println(delimiter)

	//runtime.GC()
	time.Sleep(500 * time.Millisecond)

	startTimeGo := time.Now()
	resultsGo := runWithGoroutines(numGoroutines, iterationsPerGoroutine, fibN, calculateSumOfFibsGo)
	durationGo := time.Since(startTimeGo)
	fmt.Printf("go time: \t\t%v\n", durationGo)
	fmt.Printf("go total sum: \t\t%d\n", sumResults(resultsGo))
	fmt.Println(delimiter)
	fmt.Printf("cgo time:\t\t%v\ngo time:\t\t%v\n", durationCGO, durationGo)
	fmt.Println(delimiter)
	if durationCGO < durationGo {
		fmt.Printf("cgo wins with diff: \t%v\n", durationGo-durationCGO)
	} else {
		fmt.Printf("go wins with diff: \t%v\n", durationCGO-durationGo)
	}
}

func runWithGoroutines(numWorkers, count, n int, calcFunc func(int, int) int64) []int64 {
	var wg sync.WaitGroup
	resultsChan := make(chan int64, numWorkers)

	for i := 0; i < numWorkers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			result := calcFunc(count, n)
			resultsChan <- result
		}()
	}

	// wait here for others to complete
	go func() {
		wg.Wait()
		close(resultsChan)
	}()

	results := make([]int64, 0, numWorkers)
	for result := range resultsChan {
		results = append(results, result)
	}

	return results
}

func sumResults(results []int64) int64 {
	var total int64
	for _, r := range results {
		total += r
	}
	return total
}
